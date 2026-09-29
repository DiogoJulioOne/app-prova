package com.example.gametracker.telas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gametracker.MainActivity
import com.example.gametracker.R
import com.example.gametracker.dados.UsuarioRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val usuarios by lazy { UsuarioRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val nome = findViewById<EditText>(R.id.editNomeUsuario)
        val senha = findViewById<EditText>(R.id.editSenha)
        val botaoEntrar = findViewById<Button>(R.id.buttonEntrar)

        lifecycleScope.launch {
            if (usuarios.usuarioDaSessao() != null) abrirLista()
        }

        findViewById<Button>(R.id.buttonEntrar).setOnClickListener {
            val nomeDigitado = nome.text.toString().trim()
            val senhaDigitada = senha.text.toString()
            if (nomeDigitado.isBlank() || senhaDigitada.isBlank()) {
                Toast.makeText(this, R.string.erro_campos_obrigatorios, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            botaoEntrar.isEnabled = false
            lifecycleScope.launch {
                val usuario = usuarios.autenticar(nomeDigitado, senhaDigitada)
                if (usuario == null) {
                    Toast.makeText(this@LoginActivity, R.string.erro_login, Toast.LENGTH_SHORT).show()
                    botaoEntrar.isEnabled = true
                } else {
                    usuarios.iniciarSessao(usuario.id)
                    abrirLista()
                }
            }
        }

        findViewById<Button>(R.id.buttonIrCadastro).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CadastroActivity::class.java
                )
            )
        }
    }

    private fun abrirLista() {
        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )
        finish()
    }
}