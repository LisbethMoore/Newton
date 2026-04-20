package com.lisbeth.newton

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View


class SimulacionSegundaLeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // estados

    enum class EstadoSimulacion {
        NEUTRO,                // Antes de calcular
        SIN_ACELERACION,       // a = 0, objeto quieto o velocidad constante
        ACELERACION_POSITIVA,  // a > 0, objeto gana velocidad
        DESACELERACION,        // a < 0, objeto pierde velocidad hasta detenerse
        REPOSO_TOTAL           // Fuerza insuficiente, v0=0 → sin movimiento
    }

    // variables estados

    private var estadoActual: EstadoSimulacion = EstadoSimulacion.NEUTRO
    private var posicionBloque: Float = 0f
    private var velocidadActual: Float = 0f       // píxeles/frame
    private var velocidadInicialPx: Float = 0f    // velocidad inicial en píxeles/frame

    // Valores físicos reales (para mostrar en panel)
    private var valorMasa: Float = 1f
    private var valorFuerzaAplicada: Float = 0f
    private var valorFriccionMaxima: Float = 0f
    private var valorFuerzaNeta: Float = 0f
    private var valorAceleracion: Float = 0f
    private var valorVelocidadInicial: Float = 0f
    private var valorVelocidadFinal: Float = 0f
    private var valorTiempo: Float = 1f
    private var nombreSuperficie: String = ""
    private var textoEstado: String = ""

    private var velocidadMostrada: Float = 0f
    private var desplazamientoMostrado: Float = 0f

    private var fuerzaAplicadaVisual: Float = 0f
    private var friccionVisual: Float = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var animando = false

    // pintura

    private val pintaSuelo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4B5563"); style = Paint.Style.FILL
    }
    private val pintaRayado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280"); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }

    // Colores de bloque según estado
    private val pintaBloqueAzul = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E3A8A"); style = Paint.Style.FILL
    }
    private val pintaBloqueVerde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15803D"); style = Paint.Style.FILL
    }
    private val pintaBloqueNaranja = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EA580C"); style = Paint.Style.FILL
    }
    private val pintaBloqueMorado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C3AED"); style = Paint.Style.FILL
    }
    private val pintaBordeBloque = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93C5FD"); style = Paint.Style.STROKE; strokeWidth = 2.5f
    }

    private val pintaFlechaFuerza = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#16A34A"); style = Paint.Style.STROKE
        strokeWidth = 5f; strokeCap = Paint.Cap.ROUND
    }
    private val pintaFlechaFriccion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DC2626"); style = Paint.Style.STROKE
        strokeWidth = 5f; strokeCap = Paint.Cap.ROUND
    }

    private val pintaTextoEstado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val pintaTextoNeutro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280"); textSize = 28f; textAlign = Paint.Align.CENTER
    }
    private val pintaEtiquetaVerde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15803D"); textSize = 22f
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val pintaEtiquetaRoja = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B91C1C"); textSize = 22f
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val pintaValores = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f; typeface = Typeface.MONOSPACE
    }
    fun configurarSimulacion(
        estado: EstadoSimulacion,
        texto: String,
        fuerzaAplicada: Float,
        friccionMaxima: Float,
        fuerzaNeta: Float,
        aceleracion: Float,
        masa: Float,
        velocidadInicialMs: Float,
        velocidadFinalMs: Float,
        tiempo: Float,
        superficie: String
    ) {
        estadoActual          = estado
        textoEstado           = texto
        valorFuerzaAplicada   = fuerzaAplicada
        valorFriccionMaxima   = friccionMaxima
        valorFuerzaNeta       = fuerzaNeta
        valorAceleracion      = aceleracion
        valorMasa             = masa
        valorVelocidadInicial = velocidadInicialMs
        valorVelocidadFinal   = velocidadFinalMs
        valorTiempo           = tiempo
        nombreSuperficie      = superficie
        velocidadInicialPx = when (estado) {
            EstadoSimulacion.REPOSO_TOTAL        -> 0f
            EstadoSimulacion.SIN_ACELERACION     -> if (velocidadInicialMs > 0f) 3f else 0f
            EstadoSimulacion.ACELERACION_POSITIVA -> 1.5f  // arranca lento, va acelerando
            EstadoSimulacion.DESACELERACION       -> 6f    // arranca rápido, frena
            EstadoSimulacion.NEUTRO               -> 0f
        }
        velocidadActual       = velocidadInicialPx
        velocidadMostrada     = velocidadInicialMs
        desplazamientoMostrado = 0f

        fuerzaAplicadaVisual = normalizarFuerza(fuerzaAplicada)
        friccionVisual       = normalizarFuerza(friccionMaxima)

        posicionBloque = if (width > 0) width / 2f else 0f

        detenerAnimacion()
        if (velocidadActual > 0f || estado == EstadoSimulacion.ACELERACION_POSITIVA) {
            iniciarAnimacion()
        }

        invalidate()
    }

    fun reiniciar() {
        estadoActual           = EstadoSimulacion.NEUTRO
        textoEstado            = ""
        velocidadActual        = 0f
        velocidadMostrada      = 0f
        desplazamientoMostrado = 0f
        fuerzaAplicadaVisual   = 0f
        friccionVisual         = 0f
        detenerAnimacion()
        invalidate()
    }

    private fun iniciarAnimacion() {
        animando = true
        handler.post(runnableAnimacion)
    }

    private fun detenerAnimacion() {
        animando = false
        handler.removeCallbacks(runnableAnimacion)
    }

    private val runnableAnimacion = object : Runnable {
        override fun run() {
            if (!animando) return

            val anchoBloque = width * 0.18f

            when (estadoActual) {

                EstadoSimulacion.SIN_ACELERACION -> {
                    // Movimiento uniforme: velocidad fija
                    posicionBloque    += velocidadActual
                    velocidadMostrada  = valorVelocidadInicial  // no cambia
                    // Desplazamiento acumulado real: d = v0 * t_animación
                    desplazamientoMostrado += (valorVelocidadInicial * 0.016f)
                    if (posicionBloque > width + anchoBloque) {
                        posicionBloque         = -anchoBloque
                        desplazamientoMostrado = 0f
                    }
                }

                EstadoSimulacion.ACELERACION_POSITIVA -> {
                    // Acelera gradualmente proporcional a a = ΣF/m
                    val incremento   = (valorAceleracion / valorMasa).coerceIn(0.01f, 0.4f)
                    velocidadActual  = minOf(velocidadActual + incremento, 20f)
                    posicionBloque  += velocidadActual

                    // v mostrada sube de v0 hasta vf
                    velocidadMostrada       = minOf(velocidadMostrada + 0.06f, valorVelocidadFinal)
                    desplazamientoMostrado += (velocidadMostrada * 0.016f)

                    if (posicionBloque > width + anchoBloque) {
                        posicionBloque         = -anchoBloque
                        velocidadActual        = velocidadInicialPx
                        velocidadMostrada      = valorVelocidadInicial
                        desplazamientoMostrado = 0f
                    }
                }

                EstadoSimulacion.DESACELERACION -> {
                    if (velocidadActual > 0f) {
                        val decremento   = (Math.abs(valorAceleracion) / valorMasa)
                            .toFloat().coerceIn(0.02f, 0.3f)
                        velocidadActual  = maxOf(0f, velocidadActual - decremento)
                        posicionBloque  += velocidadActual

                        velocidadMostrada       = maxOf(0f, velocidadMostrada - 0.05f)
                        desplazamientoMostrado += (velocidadMostrada * 0.016f)
                    } else {
                        velocidadActual        = 0f
                        velocidadMostrada      = 0f
                        detenerAnimacion()
                    }
                }

                else -> { /* REPOSO_TOTAL y NEUTRO: sin movimiento */ }
            }

            invalidate()
            if (animando) handler.postDelayed(this, 16)
        }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        if (posicionBloque == 0f && estadoActual != EstadoSimulacion.NEUTRO) {
            posicionBloque = w / 2f
        }

        when (estadoActual) {
            EstadoSimulacion.NEUTRO -> dibujarNeutro(canvas, w, h)
            else                   -> dibujarSimulacion(canvas, w, h)
        }
    }

    private fun dibujarNeutro(canvas: Canvas, w: Float, h: Float) {
        canvas.drawColor(Color.parseColor("#F0F4FF"))
        val pintaLinea = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 2f }
        canvas.drawLine(0f, h * 0.72f, w, h * 0.72f, pintaLinea)
        pintaTextoNeutro.textSize = 27f
        canvas.drawText("Ingrese los datos y presione", w / 2f, h * 0.38f, pintaTextoNeutro)
        canvas.drawText("Calcular para ver la simulación", w / 2f, h * 0.52f, pintaTextoNeutro)
    }

    private fun dibujarSimulacion(canvas: Canvas, w: Float, h: Float) {
        canvas.drawColor(Color.parseColor("#F0F4FF"))

        val sueloY       = h * 0.68f
        val alturaBloque = h * 0.25f
        val anchoBloque  = w * 0.18f
        val bloqueY      = sueloY - alturaBloque

        canvas.drawRect(RectF(0f, sueloY, w, h), pintaSuelo)
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, sueloY, x - 18f, h, pintaRayado); x += 28f
        }

        if (nombreSuperficie.isNotEmpty()) {
            val pintaSuperficie = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D1D5DB"); textSize = 20f; textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Superficie: $nombreSuperficie", w / 2f, sueloY + 22f, pintaSuperficie)
        }

        val bloqueIzq = posicionBloque - anchoBloque / 2f
        val bloqueDer = posicionBloque + anchoBloque / 2f
        val rectBloque = RectF(bloqueIzq, bloqueY, bloqueDer, sueloY)

        val pintaBloqueActual = when (estadoActual) {
            EstadoSimulacion.SIN_ACELERACION      -> pintaBloqueAzul
            EstadoSimulacion.ACELERACION_POSITIVA -> pintaBloqueMorado
            EstadoSimulacion.DESACELERACION       -> pintaBloqueNaranja
            EstadoSimulacion.REPOSO_TOTAL         -> pintaBloqueVerde
            else                                  -> pintaBloqueAzul
        }

        canvas.save()
        canvas.clipRect(0f, 0f, w, h)
        canvas.drawRoundRect(rectBloque, 8f, 8f, pintaBloqueActual)
        canvas.drawRoundRect(rectBloque, 8f, 8f, pintaBordeBloque)
        canvas.restore()

        // Letra "m"
        val pintaLetra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = alturaBloque * 0.38f
            textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("m", posicionBloque, bloqueY + alturaBloque / 2f + pintaLetra.textSize / 3f, pintaLetra)

        // --- Flechas ---
        val flechaY = bloqueY + alturaBloque / 2f

        // Flecha fuerza →
        if (valorFuerzaAplicada > 0f) {
            val inicioF = bloqueDer + 6f
            val finF    = inicioF + fuerzaAplicadaVisual.coerceAtLeast(26f)
            canvas.drawLine(inicioF, flechaY, finF - 14f, flechaY, pintaFlechaFuerza)
            dibujarPuntaFlecha(canvas, finF, flechaY, true, pintaFlechaFuerza)
            pintaEtiquetaVerde.textSize = 21f
            canvas.drawText("F=${"%.1f".format(valorFuerzaAplicada)}N",
                (inicioF + finF) / 2f, flechaY - 12f, pintaEtiquetaVerde)
        }
        // Flecha fricción ←
        if (valorFriccionMaxima > 0f) {
            val inicioFric = bloqueIzq - 6f
            val finFric    = inicioFric - friccionVisual.coerceAtLeast(26f)
            canvas.drawLine(inicioFric, flechaY, finFric + 14f, flechaY, pintaFlechaFriccion)
            dibujarPuntaFlecha(canvas, finFric, flechaY, false, pintaFlechaFriccion)
            pintaEtiquetaRoja.textSize = 21f
            canvas.drawText("f=${"%.1f".format(valorFriccionMaxima)}N",
                (inicioFric + finFric) / 2f, flechaY - 12f, pintaEtiquetaRoja)
        }

        // estado
        pintaTextoEstado.color = when (estadoActual) {
            EstadoSimulacion.DESACELERACION       -> Color.parseColor("#EA580C")
            EstadoSimulacion.ACELERACION_POSITIVA -> Color.parseColor("#7C3AED")
            EstadoSimulacion.REPOSO_TOTAL         -> Color.parseColor("#1E3A8A")
            else                                  -> Color.parseColor("#065F46")
        }
        canvas.drawText(textoEstado, w / 2f, h * 0.11f, pintaTextoEstado)

        // --- Panel de valores ---
        dibujarPanelValores(canvas, w, h)
    }
    private fun dibujarPanelValores(canvas: Canvas, w: Float, h: Float) {
        val pintaFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255); style = Paint.Style.FILL
        }
        val pintaBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1"); style = Paint.Style.STROKE; strokeWidth = 1f
        }

        val margen = 10f
        val panelH = 96f
        val panelW = 220f
        val rect = RectF(margen, h - panelH - margen, margen + panelW, h - margen)
        canvas.drawRoundRect(rect, 8f, 8f, pintaFondo)
        canvas.drawRoundRect(rect, 8f, 8f, pintaBorde)

        pintaValores.textSize = 21f
        val col1 = margen + 8f
        val col2 = margen + 116f
        val f1   = h - panelH - margen + 24f
        val f2   = f1 + 24f
        val f3   = f2 + 24f
        val f4   = f3 + 24f

        // Fila 1: ΣF y a
        pintaValores.color = Color.parseColor("#7C3AED")
        canvas.drawText("ΣF=${"%.2f".format(valorFuerzaNeta)}N", col1, f1, pintaValores)
        pintaValores.color = Color.parseColor("#B45309")
        canvas.drawText("a=${"%.2f".format(valorAceleracion)}m/s²", col2, f1, pintaValores)

        // Fila 2: v en tiempo real y desplazamiento acumulado
        pintaValores.color = Color.parseColor("#0369A1")
        canvas.drawText("v=${"%.2f".format(velocidadMostrada)}m/s", col1, f2, pintaValores)
        pintaValores.color = Color.parseColor("#064E3B")
        canvas.drawText("d=${"%.2f".format(desplazamientoMostrado)}m", col2, f2, pintaValores)

        // Fila 3: vf calculada
        pintaValores.color = Color.parseColor("#374151")
        canvas.drawText("vf=${"%.2f".format(valorVelocidadFinal)}m/s", col1, f3, pintaValores)

        // Fila 4: indicador textual
        pintaValores.color = when (estadoActual) {
            EstadoSimulacion.DESACELERACION       -> Color.parseColor("#DC2626")
            EstadoSimulacion.ACELERACION_POSITIVA -> Color.parseColor("#7C3AED")
            EstadoSimulacion.REPOSO_TOTAL         -> Color.parseColor("#1E3A8A")
            else                                  -> Color.parseColor("#065F46")
        }
        val indicador = when (estadoActual) {
            EstadoSimulacion.REPOSO_TOTAL         -> "▪ Sin movimiento"
            EstadoSimulacion.SIN_ACELERACION      -> "▶ Vel. constante"
            EstadoSimulacion.ACELERACION_POSITIVA -> "▶▶ Acelerando"
            EstadoSimulacion.DESACELERACION       -> "▪▪ Frenando..."
            EstadoSimulacion.NEUTRO               -> ""
        }
        canvas.drawText(indicador, col1, f4, pintaValores)
    }

    private fun dibujarPuntaFlecha(
        canvas: Canvas, x: Float, y: Float,
        haciaRight: Boolean, pinta: Paint
    ) {
        val t = 15f
        val path = Path()
        if (haciaRight) {
            path.moveTo(x, y); path.lineTo(x - t, y - t * 0.55f); path.lineTo(x - t, y + t * 0.55f)
        } else {
            path.moveTo(x, y); path.lineTo(x + t, y - t * 0.55f); path.lineTo(x + t, y + t * 0.55f)
        }
        path.close()
        canvas.drawPath(path, Paint(pinta).apply { style = Paint.Style.FILL })
    }

    private fun normalizarFuerza(f: Float): Float {
        if (f <= 0f) return 0f
        return (f * 3f).coerceIn(24f, 100f)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detenerAnimacion()
    }
}