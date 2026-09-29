package com.example.gametracker.telas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gametracker.R
import com.example.gametracker.dados.DadosSociaisMock

class ChatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)
        findViewById<Button>(R.id.buttonVoltarChats).setOnClickListener { finish() }
        val lista = findViewById<LinearLayout>(R.id.listaConversas)
        DadosSociaisMock.conversas().forEach { (jogador, ultimaMensagem) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = getDrawable(R.drawable.bg_card_jogo)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(
                        Intent(this@ChatsActivity, ChatActivity::class.java)
                            .putExtra(ChatActivity.EXTRA_JOGADOR_ID, jogador.id)
                    )
                }
            }
            card.addView(TextView(this).apply {
                text = jogador.nome
                textSize = 17f
                setTextColor(getColor(R.color.texto))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            val preview = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            preview.addView(TextView(this).apply {
                text = ultimaMensagem?.texto ?: getString(R.string.chat_sem_mensagens)
                textSize = 14f
                setTextColor(getColor(R.color.texto_secundario))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(0, -2, 1f))
            preview.addView(TextView(this).apply {
                text = ultimaMensagem?.horario.orEmpty()
                textSize = 12f
                setTextColor(getColor(R.color.destaque))
                setPadding(dp(8), 0, 0, 0)
            })
            card.addView(preview)
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(12)
            lista.addView(card, params)
        }
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}
