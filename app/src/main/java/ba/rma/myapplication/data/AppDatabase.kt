package ba.rma.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database for the app. It has two tables: owned_stickers and catalog_players.
 *
 * `version = 2`: we bumped from 1 to 2 when we added the catalog cache + acquiredAt.
 * `exportSchema = false`: we don't export the schema to a file (keeps the student project simple).
 *
 * We use `fallbackToDestructiveMigration(true)`: if the schema changes, Room may just rebuild the
 * tables (losing data) instead of needing a hand-written migration. That is acceptable here because
 * everything stored is either a cache (re-downloadable) or easily re-collected.
 */
@Database(entities = [OwnedSticker::class, PlayerEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao

    companion object {
        // @Volatile makes sure all threads see the latest value of INSTANCE (no stale cache).
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the one shared database, creating it the first time. Classic double-checked
         * locking so the database is built only once even if two threads ask at the same time.
         */
        fun getInstance(context: Context): AppDatabase {
            val existing = INSTANCE
            if (existing != null) {
                return existing
            }
            synchronized(this) {
                val again = INSTANCE
                if (again != null) {
                    return again
                }
                val created = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "slicice.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = created
                return created
            }
        }
    }
}
