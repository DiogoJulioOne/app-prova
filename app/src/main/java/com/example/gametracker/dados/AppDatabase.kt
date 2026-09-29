package com.example.gametracker.dados

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gametracker.modelos.Jogo
import com.example.gametracker.modelos.Usuario

@Database(
    entities = [Jogo::class, Usuario::class, DatabaseMigration::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jogoBancoDados(): JogoBancoDados
    abstract fun usuarioBancoDados(): UsuarioBancoDados

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obter(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build().also { instancia = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `usuarios` (" +
                        "`id` TEXT NOT NULL, " +
                        "`nome` TEXT NOT NULL, " +
                        "`usuario` TEXT NOT NULL, " +
                        "`senhaSalt` TEXT NOT NULL, " +
                        "`senhaHash` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_usuarios_usuario` " +
                        "ON `usuarios` (`usuario`)"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO `usuarios` " +
                        "(`id`, `nome`, `usuario`, `senhaSalt`, `senhaHash`) " +
                        "VALUES ('legacy-jogador', 'Jogador', '__legacy_jogador__', '', '')"
                )
                database.execSQL(
                    "CREATE TABLE `jogos_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`titulo` TEXT NOT NULL, " +
                        "`plataforma` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`dataInicio` TEXT NOT NULL, " +
                        "`horasJogadas` REAL, " +
                        "`progresso` INTEGER, " +
                        "`observacoes` TEXT NOT NULL, " +
                        "`usuarioId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL(
                    "INSERT INTO `jogos_new` " +
                        "(`id`, `titulo`, `plataforma`, `status`, `dataInicio`, " +
                        "`horasJogadas`, `progresso`, `observacoes`, `usuarioId`) " +
                        "SELECT `id`, `titulo`, `plataforma`, `status`, `dataInicio`, " +
                        "`horasJogadas`, `progresso`, `observacoes`, 'legacy-jogador' FROM `jogos`"
                )
                database.execSQL("DROP TABLE `jogos`")
                database.execSQL("ALTER TABLE `jogos_new` RENAME TO `jogos`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_jogos_usuarioId` ON `jogos` (`usuarioId`)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // adiciona novos campos de perfil mantendo dados existentes
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `nickname` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `email` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `bio` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `jogoFavorito` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `avatar` TEXT NOT NULL DEFAULT 'avatar_1'")
            }
        }

        private const val DATABASE_NAME = "gametracker.db"
    }
}
