package com.example.gametracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gametracker.dados.BibliotecaSugestoes
import com.example.gametracker.dados.JogoRepository
import com.example.gametracker.dados.UsuarioRepository
import com.example.gametracker.modelos.Jogo
import com.example.gametracker.telas.AdicionarJogoActivity
import com.example.gametracker.telas.DetalhesJogoActivity
import com.example.gametracker.telas.LoginActivity
import com.example.gametracker.telas.PerfilActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
class MainActivity : AppCompatActivity(), SensorEventListener {

    private val jogos by lazy { JogoRepository(this) }
    private val usuarios by lazy { UsuarioRepository(this) }

    private lateinit var listaJogos: LinearLayout
    private lateinit var textoVazio: TextView
    private lateinit var textoContagem: TextView
    private lateinit var editPesquisaJogos: EditText
    private var filtroStatusSelecionado = FILTRO_TODOS
    private var carregamentoLista: Job? = null
    private var usuarioId: String? = null
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null

    private var telaParaBaixo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager

        acelerometro =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        listaJogos = findViewById(R.id.listaJogos)
        textoVazio = findViewById(R.id.textoVazio)
        textoContagem = findViewById(R.id.textoContagem)
        editPesquisaJogos = findViewById(R.id.editPesquisaJogos)

        configurarFiltros()
        editPesquisaJogos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                atualizarLista()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        findViewById<Button>(R.id.buttonAdicionarJogo).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AdicionarJogoActivity::class.java
                )
            )
        }

        findViewById<Button>(R.id.buttonMatch).setOnClickListener {
            startActivity(Intent(this, com.example.gametracker.telas.MatchActivity::class.java))
        }
        findViewById<Button>(R.id.buttonChats).setOnClickListener {
            startActivity(Intent(this, com.example.gametracker.telas.ChatsActivity::class.java))
        }
        findViewById<Button>(R.id.buttonPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        findViewById<Button>(R.id.buttonSair).setOnClickListener {
            usuarios.encerrarSessao()
            voltarAoLogin()
        }

        lifecycleScope.launch {
            val usuario = usuarios.usuarioDaSessao()
            if (usuario == null) {
                voltarAoLogin()
                return@launch
            }
            usuarioId = usuario.id
            findViewById<TextView>(R.id.textoBoasVindas).text =
                getString(R.string.boas_vindas, usuario.nome)
            atualizarLista()
        }
    }

    override fun onResume() {
        super.onResume()

        acelerometro?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        if (::listaJogos.isInitialized) {
            atualizarLista()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
    private fun atualizarLista() {
        val donoId = usuarioId ?: return
        carregamentoLista?.cancel()
        carregamentoLista = lifecycleScope.launch {
            val todosOsJogos = jogos.listar(donoId)
            val consulta = editPesquisaJogos.text.toString().trim()
            val filtroAtual = filtroStatusSelecionado
            val lista = todosOsJogos.filter { jogo ->
                val correspondeAoTitulo = jogo.titulo.contains(consulta, ignoreCase = true)
                val correspondeAoStatus = when (filtroAtual) {
                    FILTRO_TODOS -> true
                    FILTRO_PAUSADO -> jogo.status.equals(FILTRO_PAUSADO, ignoreCase = true)
                    FILTRO_ZERADO -> jogo.status.equals(FILTRO_ZERADO, ignoreCase = true) ||
                        jogo.status.equals(STATUS_CONCLUIDO_ANTERIOR, ignoreCase = true)
                    else -> jogo.status.equals(filtroAtual, ignoreCase = true)
                }
                correspondeAoTitulo && correspondeAoStatus
            }

            listaJogos.removeAllViews()

            textoContagem.text = resources.getQuantityString(
                R.plurals.contagem_jogos,
                lista.size,
                lista.size
            )

            textoVazio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            textoVazio.setText(
                if (todosOsJogos.isEmpty()) R.string.colecao_vazia
                else R.string.nenhum_jogo_encontrado
            )

            lista.forEach { jogo ->

                val card = LayoutInflater.from(this@MainActivity)
                    .inflate(
                        R.layout.item_jogo,
                        listaJogos,
                        false
                    )

                card.findViewById<TextView>(
                    R.id.textoTituloJogo
                ).text = jogo.titulo

                card.findViewById<TextView>(
                    R.id.textoPlataformaJogo
                ).text = jogo.plataforma

                card.findViewById<TextView>(
                    R.id.textoStatusJogo
                ).text = jogo.status

                card.setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, DetalhesJogoActivity::class.java)
                            .putExtra(DetalhesJogoActivity.EXTRA_ID_JOGO, jogo.id)
                    )
                }

                card.findViewById<Button>(
                    R.id.buttonEditarJogo
                ).setOnClickListener {

                    startActivity(
                        Intent(
                            this@MainActivity,
                            AdicionarJogoActivity::class.java
                        ).putExtra(
                            AdicionarJogoActivity.EXTRA_ID_JOGO,
                            jogo.id
                        )
                    )
                }

                card.findViewById<Button>(
                    R.id.buttonExcluirJogo
                ).setOnClickListener {

                    confirmarExclusao(jogo)
                }

                listaJogos.addView(card)
            }
        }
    }

    private fun configurarFiltros() {
        val filtros = listOf(
            FILTRO_TODOS to findViewById<Button>(R.id.filtroTodos),
            FILTRO_JOGANDO to findViewById<Button>(R.id.filtroJogando),
            FILTRO_PAUSADO to findViewById<Button>(R.id.filtroPausado),
            FILTRO_ZERADO to findViewById<Button>(R.id.filtroZerado),
            FILTRO_QUERO_JOGAR to findViewById<Button>(R.id.filtroQueroJogar)
        )
        filtros.forEach { (filtro, botao) ->
            botao.setOnClickListener {
                filtroStatusSelecionado = filtro
                filtros.forEach { (opcao, item) ->
                    item.setBackgroundResource(
                        if (opcao == filtroStatusSelecionado) {
                            R.drawable.bg_botao_principal
                        } else {
                            R.drawable.bg_botao_secundario
                        }
                    )
                }
                atualizarLista()
            }
        }
    }

    private fun confirmarExclusao(jogo: Jogo) {

        AlertDialog.Builder(this)
            .setTitle(R.string.confirmar_exclusao_titulo)
            .setMessage(
                getString(
                    R.string.confirmar_exclusao_mensagem,
                    jogo.titulo
                )
            )
            .setNegativeButton(
                R.string.cancelar,
                null
            )
            .setPositiveButton(
                R.string.excluir
            ) { _, _ ->
                lifecycleScope.launch {
                    usuarioId?.let { jogos.excluir(jogo.id, it) }
                    atualizarLista()
                }
            }
            .show()
    }

    private fun voltarAoLogin() {

        startActivity(
            Intent(
                this,
                LoginActivity::class.java
            )
        )

        finish()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            if (z < -7f && !telaParaBaixo) {
                telaParaBaixo = true
                val jogoSorteado = BibliotecaSugestoes.jogos.random()
                AlertDialog.Builder(this)
                    .setTitle("🎮 Jogo sorteado")
                    .setMessage("Que tal jogar hoje: $jogoSorteado?")
                    .setPositiveButton("OK", null)
                    .show()
            } else if (z > -3f) {
                telaParaBaixo = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
    private companion object {
        const val FILTRO_TODOS = "Todos"
        const val FILTRO_JOGANDO = "Jogando"
        const val FILTRO_PAUSADO = "Pausado"
        const val FILTRO_ZERADO = "Zerado"
        const val FILTRO_QUERO_JOGAR = "Quero jogar"
        const val STATUS_CONCLUIDO_ANTERIOR = "Concluído"

    }
}