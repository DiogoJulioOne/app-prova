package com.example.gametracker.dados

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_migrations")
data class DatabaseMigration(
    @PrimaryKey val migrationKey: String
)
