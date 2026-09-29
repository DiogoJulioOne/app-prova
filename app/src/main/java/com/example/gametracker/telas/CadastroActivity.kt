package com.example.gametracker.telas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gametracker.MainActivity
import com.example.gametracker.R
import com.example.gametracker.dados.UsuarioRepository
import kotlinx.coroutines.launch

class CadastroActivity : AppCompatActivity() {
    private val usuarios by lazy { UsuarioRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        val btnVoltar = findViewById<ImageButton>(R.id.btnVoltar)
        val nome = findViewById<EditText>(R.id.editNomeUsuario)
        val senha = findViewById<EditText>(R.id.editSenha)
        val confirmarSenha = findViewById<EditText>(R.id.editConfirmarSenha)
        val botaoCadastrar = findViewById<Button>(R.id.buttonCadastrar)
        val textJaTemConta = findViewById<TextView>(R.id.textJaTemConta)

        btnVoltar.setOnClickListener {
            voltarParaLogin()
        }

        textJaTemConta.setOnClickListener {
            voltarParaLogin()
        }

        botaoCadastrar.setOnClickListener {
            val nomeDigitado = nome.text.toString().trim()
            val senhaDigitada = senha.text.toString()
            when {
                nomeDigitado.isBlank() || senhaDigitada.isBlank() ||
                    confirmarSenha.text.toString().isBlank() ->
                    mostrarMensagem(R.string.erro_campos_obrigatorios)
                nomeDigitado.length < 3 ->
                    mostrarMensagem(R.string.erro_nome_curto)
                senhaDigitada.length < 6 ->
                    mostrarMensagem(R.string.erro_senha_curta)
                senhaDigitada != confirmarSenha.text.toString() ->
                    mostrarMensagem(R.string.erro_senha_diferente)
                else -> {
                    botaoCadastrar.isEnabled = false
                    lifecycleScope.launch {
                        val usuario = usuarios.cadastrar(nomeDigitado, senhaDigitada)
                        if (usuario == null) {
                            mostrarMensagem(R.string.erro_usuario_existente)
                            botaoCadastrar.isEnabled = true
                        } else {
                            usuarios.iniciarSessao(usuario.id)
                            startActivity(Intent(this@CadastroActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun voltarParaLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun mostrarMensagem(mensagem: Int) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }
}
