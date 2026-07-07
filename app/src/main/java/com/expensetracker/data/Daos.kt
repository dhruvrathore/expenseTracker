package com.expensetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE month = :month LIMIT 1")
    fun observeBudget(month: String): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets WHERE month = :month LIMIT 1")
    suspend fun getBudget(month: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets WHERE month < :month ORDER BY month DESC LIMIT 1")
    suspend fun latestBudgetBefore(month: String): MonthlyBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: MonthlyBudgetEntity)

    /** All months that have any data (budgets or transactions), most-recent first. */
    @Query(
        "SELECT month FROM monthly_budgets " +
            "UNION SELECT month FROM transactions " +
            "ORDER BY month DESC"
    )
    fun observeAvailableMonths(): Flow<List<String>>
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE month = :month ORDER BY timestamp DESC, id DESC")
    fun observeForMonth(month: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query(
        "UPDATE transactions SET amount = :amount, description = :description, " +
            "category = :category, tag = :tag WHERE id = :id"
    )
    suspend fun updateFields(id: Long, amount: Double, description: String, category: String, tag: String?)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE month = :month")
    suspend fun deleteForMonth(month: String)
}

@Dao
interface CategoryLimitDao {
    @Query("SELECT * FROM category_limits WHERE month = :month")
    fun observeForMonth(month: String): Flow<List<CategoryLimitEntity>>

    @Query("SELECT * FROM category_limits WHERE month = :month")
    suspend fun getForMonth(month: String): List<CategoryLimitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: CategoryLimitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(limits: List<CategoryLimitEntity>)
}
