package com.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.YearMonth

@Database(
    entities = [
        MonthlyBudgetEntity::class, TransactionEntity::class, CategoryLimitEntity::class,
        IncomeEntity::class, SavingsEntryEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryLimitDao(): CategoryLimitDao
    abstract fun incomeDao(): IncomeDao
    abstract fun savingsEntryDao(): SavingsEntryDao

    companion object {
        // v1 -> v2: add the per-category limits table.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_limits` " +
                        "(`category` TEXT NOT NULL, `limit_amount` REAL NOT NULL, " +
                        "PRIMARY KEY(`category`))"
                )
            }
        }

        /**
         * v2 -> v3: month-scope everything.
         * - budget (single row) -> monthly_budgets keyed by month
         * - transactions gain a `month` column, backfilled from each transaction's timestamp
         * - category_limits re-keyed by (month, category)
         * Pre-existing budget and category limits are assigned to the month the upgrade runs in.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val month = YearMonth.now().toString()

                // monthly_budgets
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `monthly_budgets` " +
                        "(`month` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, PRIMARY KEY(`month`))"
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO `monthly_budgets` (`month`, `monthlyLimit`) " +
                        "SELECT '$month', monthlyLimit FROM budget WHERE id = 0"
                )
                db.execSQL("DROP TABLE budget")

                // transactions: rebuild with a month column derived from the timestamp
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions_new` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, " +
                        "`description` TEXT NOT NULL, `category` TEXT NOT NULL, `month` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `transactions_new` (`id`, `amount`, `description`, `category`, `month`, `timestamp`) " +
                        "SELECT id, amount, description, category, " +
                        "strftime('%Y-%m', timestamp / 1000, 'unixepoch'), timestamp FROM transactions"
                )
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                // category_limits: rebuild with a composite (month, category) key
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_limits_new` " +
                        "(`month` TEXT NOT NULL, `category` TEXT NOT NULL, `limit_amount` REAL NOT NULL, " +
                        "PRIMARY KEY(`month`, `category`))"
                )
                db.execSQL(
                    "INSERT INTO `category_limits_new` (`month`, `category`, `limit_amount`) " +
                        "SELECT '$month', category, limit_amount FROM category_limits"
                )
                db.execSQL("DROP TABLE category_limits")
                db.execSQL("ALTER TABLE category_limits_new RENAME TO category_limits")
            }
        }

        // v3 -> v4: the "Bills" category was renamed to "Utilities"; carry existing rows over.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE `transactions` SET `category` = 'Utilities' WHERE `category` = 'Bills'")
                db.execSQL("UPDATE `category_limits` SET `category` = 'Utilities' WHERE `category` = 'Bills'")
            }
        }

        // v4 -> v5: add an optional free-text tag to transactions (e.g. "Ooty" on a Travel expense).
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `tag` TEXT")
            }
        }

        // v5 -> v6: add a monthly salary table, independent of the spending limit.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `salary` " +
                        "(`month` TEXT NOT NULL, `salary` REAL NOT NULL, PRIMARY KEY(`month`))"
                )
            }
        }

        // v6 -> v7: rename "salary" to "income" (table, column) to match the renamed UI section.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `income` " +
                        "(`month` TEXT NOT NULL, `income` REAL NOT NULL, PRIMARY KEY(`month`))"
                )
                db.execSQL("INSERT INTO `income` (`month`, `income`) SELECT `month`, `salary` FROM `salary`")
                db.execSQL("DROP TABLE `salary`")
            }
        }

        // v7 -> v8: add a savings/investments table (one running total per month), independent of
        // spending and income. Superseded by v8 -> v9 below, which splits it into per-entry rows.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `savings` " +
                        "(`month` TEXT NOT NULL, `savings` REAL NOT NULL, PRIMARY KEY(`month`))"
                )
            }
        }

        // v8 -> v9: replace the single monthly savings total with individual contribution entries,
        // so the Savings screen can list history instead of only showing/editing one number. Each
        // month's existing total becomes one legacy entry, dated to the start of that month.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `savings_entries` " +
                        "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, " +
                        "`description` TEXT NOT NULL, `month` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `savings_entries` (`amount`, `description`, `month`, `timestamp`) " +
                        "SELECT `savings`, 'Savings & Investments', `month`, " +
                        "CAST(strftime('%s', `month` || '-01') AS INTEGER) * 1000 FROM `savings`"
                )
                db.execSQL("DROP TABLE `savings`")
            }
        }

        // v9 -> v10: split each entry into Savings vs Investment, plus an optional free-text tag
        // (e.g. "FD", "MF", "Stock", "Gold"). Existing rows default to Savings with no tag.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `savings_entries` ADD COLUMN `kind` TEXT NOT NULL DEFAULT 'SAVINGS'")
                db.execSQL("ALTER TABLE `savings_entries` ADD COLUMN `tag` TEXT")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense-tracker.db"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
                ).build().also { instance = it }
            }
    }
}
