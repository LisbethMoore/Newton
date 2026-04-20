package com.lisbeth.newton

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

class SimulacionPrimeraLeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {



    enum class EstadoSimulacion {
        NEUTRO,
        REPOSO,
        EQUILIBRIO_DINAMICO,
        EQUILIBRIO_ROTO,     // La fuerza supera la friccion  entonces acelera
        DESACELERACION       // la friccion supera la fuerza entonces frena hasta detenerse
    }

    private var estadoActual: EstadoSimulacion = EstadoSimulacion.NEUTRO
    private var posicionBloque: Float = 0f

    private var velocidadActual: Float = 0f

    private var velocidadInicial: Float = 0f

    private var textoEstado: String = ""

    private var valorFuerzaAplicada: Float = 0f
    private var valorFriccionMaxima: Float = 0f
    private var valorFuerzaNeta: Float = 0f
    private var valorAceleracion: Float = 0f
    private var valorMasa: Float = 1f
    private var valorVelocidadInicial: Float = 0f

    private var velocidadMostrada: Float = 0f

    private var fuerzaAplicadaVisual: Float = 0f
    private var friccionVisual: Float = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var animando = false


    private val pintaSuelo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4B5563")
        style = Paint.Style.FILL
    }

    private val pintaRayadoSuelo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val pintaBloque = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E3A8A")
        style = Paint.Style.FILL
    }

    private val pintaBordeBloque = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93C5FD")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val pintaBloqueDesacel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EA580C")
        style = Paint.Style.FILL
    }

    private val pintaFuerzaAplicada = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#16A34A")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val pintaFriccion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DC2626")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val pintaTextoEstado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E3A8A")
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val pintaTextoNeutro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val pintaValores = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#065F46")
        textSize = 26f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.MONOSPACE
    }

    private val pintaEtiquetaVerde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#15803D")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val pintaEtiquetaRoja = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B91C1C")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }


    fun configurarSimulacion(
        estado: EstadoSimulacion,
        texto: String,
        fuerzaAplicada: Float,
        friccionMaxima: Float,
        fuerzaNeta: Float,
        aceleracion: Float,
        masa: Float,
        velocidadInicialMs: Float
    ) {
        estadoActual            = estado
        textoEstado             = texto
        valorFuerzaAplicada     = fuerzaAplicada
        valorFriccionMaxima     = friccionMaxima
        valorFuerzaNeta         = fuerzaNeta
        valorAceleracion        = aceleracion
        valorMasa               = masa
        valorVelocidadInicial   = velocidadInicialMs
        velocidadMostrada       = velocidadInicialMs

        fuerzaAplicadaVisual = normalizarFuerza(fuerzaAplicada)
        friccionVisual       = normalizarFuerza(friccionMaxima)

        velocidadInicial = when (estado) {
            EstadoSimulacion.REPOSO              -> 0f
            EstadoSimulacion.EQUILIBRIO_DINAMICO -> 3f
            EstadoSimulacion.EQUILIBRIO_ROTO     -> 2f   // arranca lento y va acelerando
            EstadoSimulacion.DESACELERACION      -> 5f   // arranca rápido y va frenando
            EstadoSimulacion.NEUTRO              -> 0f
        }
        velocidadActual = velocidadInicial

        // Centrar el bloque
        posicionBloque = if (width > 0) width / 2f else 0f

        detenerAnimacion()
        if (velocidadActual > 0f) iniciarAnimacion()

        invalidate()
    }

    fun reiniciar() {
        estadoActual          = EstadoSimulacion.NEUTRO
        textoEstado           = ""
        fuerzaAplicadaVisual  = 0f
        friccionVisual        = 0f
        velocidadActual       = 0f
        velocidadMostrada     = 0f
        detenerAnimacion()
        invalidate()
    }

    //  bucle de animacion

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

                EstadoSimulacion.EQUILIBRIO_DINAMICO -> {
                    posicionBloque += velocidadActual
                    velocidadMostrada = valorVelocidadInicial  // constante
                    if (posicionBloque > width + anchoBloque) {
                        posicionBloque = -anchoBloque
                    }
                }

                EstadoSimulacion.EQUILIBRIO_ROTO -> {
                    val incremento = (valorFuerzaNeta / valorMasa).coerceIn(0.01f, 0.3f)
                    velocidadActual = minOf(velocidadActual + incremento, 18f)
                    posicionBloque += velocidadActual

                    velocidadMostrada = minOf(velocidadMostrada + 0.05f, 30f)

                    if (posicionBloque > width + anchoBloque) {
                        posicionBloque = -anchoBloque
                        velocidadActual = velocidadInicial
                        velocidadMostrada = 0f
                    }
                }

                EstadoSimulacion.DESACELERACION -> {
                    if (velocidadActual > 0f) {
                        val decremento = (Math.abs(valorFuerzaNeta) / valorMasa)
                            .toFloat().coerceIn(0.02f, 0.25f)
                        velocidadActual = maxOf(0f, velocidadActual - decremento)
                        posicionBloque += velocidadActual

                        velocidadMostrada = maxOf(0f, velocidadMostrada - 0.04f)
                    } else {
                        velocidadActual   = 0f
                        velocidadMostrada = 0f
                        detenerAnimacion()
                    }
                }

                else -> { /* REPOSO y NEUTRO no animan */ }
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
        val pintaLinea = Paint().apply {
            color = Color.parseColor("#CBD5E1"); strokeWidth = 2f
        }
        canvas.drawLine(0f, h * 0.72f, w, h * 0.72f, pintaLinea)
        pintaTextoNeutro.textSize = 27f
        canvas.drawText("Ingrese los datos y presione", w / 2f, h * 0.40f, pintaTextoNeutro)
        canvas.drawText("Analizar para ver la simulación", w / 2f, h * 0.55f, pintaTextoNeutro)
    }
    private fun dibujarSimulacion(canvas: Canvas, w: Float, h: Float) {
        canvas.drawColor(Color.parseColor("#F0F4FF"))

        val sueloY      = h * 0.68f
        val alturaBloque = h * 0.26f
        val anchoBloque  = w * 0.18f
        val bloqueY      = sueloY - alturaBloque

        // suelo
        canvas.drawRect(RectF(0f, sueloY, w, h), pintaSuelo)
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, sueloY, x - 18f, h, pintaRayadoSuelo)
            x += 28f
        }

        // bloque
        val bloqueIzq = posicionBloque - anchoBloque / 2f
        val bloqueDer  = posicionBloque + anchoBloque / 2f
        val rectBloque = RectF(bloqueIzq, bloqueY, bloqueDer, sueloY)

        // Color del bloque según estado: naranja para desaceleración
        val pintaBloqueActual = if (estadoActual == EstadoSimulacion.DESACELERACION)
            pintaBloqueDesacel else pintaBloque

        canvas.save()
        canvas.clipRect(0f, 0f, w, h)
        canvas.drawRoundRect(rectBloque, 8f, 8f, pintaBloqueActual)
        canvas.drawRoundRect(rectBloque, 8f, 8f, pintaBordeBloque)
        canvas.restore()

        // Letra "m" dentro del bloque
        val pintaLetra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = alturaBloque * 0.38f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("m", posicionBloque, bloqueY + alturaBloque / 2f + pintaLetra.textSize / 3f, pintaLetra)

        // flecha
        val flechaY = bloqueY + alturaBloque / 2f

        // Flecha fuerza aplicada
        val separacion = 18f

        val inicioF = bloqueDer + separacion
        val finF = inicioF + fuerzaAplicadaVisual.coerceAtLeast(35f)
        pintaFuerzaAplicada.style = Paint.Style.STROKE
        canvas.drawLine(inicioF, flechaY, finF - 16f, flechaY, pintaFuerzaAplicada)
        dibujarPuntaFlecha(canvas, finF, flechaY, true, pintaFuerzaAplicada)

        // Etiqueta F con valor real
        pintaEtiquetaVerde.textSize = 22f
        val textoF = "F = %.1f N".format(valorFuerzaAplicada)

        canvas.drawText(
            textoF,
            (inicioF + finF) / 2f,
            flechaY - 20f,
            pintaEtiquetaVerde
        )

        // Flecha friccion ←
        val inicioFric = bloqueIzq - separacion
        val finFric = inicioFric - friccionVisual.coerceAtLeast(35f)
        pintaFriccion.style = Paint.Style.STROKE
        canvas.drawLine(inicioFric, flechaY, finFric + 16f, flechaY, pintaFriccion)
        dibujarPuntaFlecha(canvas, finFric, flechaY, false, pintaFriccion)

        // Etiqueta f con valor real
        pintaEtiquetaRoja.textSize = 22f
        val textoFric = "f=%.1fN".format(valorFriccionMaxima)
        canvas.drawText(textoFric, (inicioFric + finFric) / 2f, flechaY - 20f, pintaEtiquetaRoja)

        //estadom
        pintaTextoEstado.textSize = 30f
        // color del texto según estado
        pintaTextoEstado.color = when (estadoActual) {
            EstadoSimulacion.DESACELERACION  -> Color.parseColor("#EA580C")
            EstadoSimulacion.EQUILIBRIO_ROTO -> Color.parseColor("#7C3AED")
            EstadoSimulacion.REPOSO          -> Color.parseColor("#1E3A8A")
            else                             -> Color.parseColor("#065F46")
        }
        canvas.drawText(textoEstado, w / 2f, h * 0.12f, pintaTextoEstado)

        dibujarPanelValores(canvas, w, h)
    }


    private fun dibujarPanelValores(canvas: Canvas, w: Float, h: Float) {

        val pintaFondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            style = Paint.Style.FILL
        }

        val margen = 10f
        val panelH = 118f
        val panelW = 230f
        val panelRect = RectF(
            margen,
            h - panelH - margen,
            margen + panelW,
            h - margen
        )
        canvas.drawRoundRect(panelRect, 8f, 8f, pintaFondo)

        // Borde del panel
        val pintaBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(panelRect, 8f, 8f, pintaBorde)

        pintaValores.textSize = 21f

        val x = margen + 10f
        var y = h - panelH - margen + 22f
        val espacio = 24f

        // ΣF
        pintaValores.color = Color.parseColor("#7C3AED")
        canvas.drawText("ΣF = %.2f N".format(valorFuerzaNeta), x, y, pintaValores)

        y += espacio

        // a
        pintaValores.color = Color.parseColor("#B45309")
        canvas.drawText("a = %.2f m/s²".format(valorAceleracion), x, y, pintaValores)

        y += espacio

        // v
        pintaValores.color = Color.parseColor("#0369A1")
        canvas.drawText("v = %.2f m/s".format(velocidadMostrada), x, y, pintaValores)

        y += espacio

        // indica de estado
        pintaValores.color = when (estadoActual) {
            EstadoSimulacion.DESACELERACION  -> Color.parseColor("#DC2626")
            EstadoSimulacion.EQUILIBRIO_ROTO -> Color.parseColor("#7C3AED")
            EstadoSimulacion.REPOSO          -> Color.parseColor("#1E3A8A")
            else                             -> Color.parseColor("#065F46")
        }

        val indicador = when (estadoActual) {
            EstadoSimulacion.REPOSO -> "Sin movimiento"
            EstadoSimulacion.EQUILIBRIO_DINAMICO -> "Velocidad constante"
            EstadoSimulacion.EQUILIBRIO_ROTO -> "Acelerando"
            EstadoSimulacion.DESACELERACION ->
                if (velocidadMostrada <= 0f) "Detenido" else "Frenando..."
            EstadoSimulacion.NEUTRO -> ""
        }

        canvas.drawText(indicador, x, y, pintaValores)
    }

    private fun dibujarPuntaFlecha(
        canvas: Canvas, x: Float, y: Float,
        haciaRight: Boolean, pinta: Paint
    ) {
        val t = 16f
        val path = Path()
        if (haciaRight) {
            path.moveTo(x, y)
            path.lineTo(x - t, y - t * 0.55f)
            path.lineTo(x - t, y + t * 0.55f)
        } else {
            path.moveTo(x, y)
            path.lineTo(x + t, y - t * 0.55f)
            path.lineTo(x + t, y + t * 0.55f)
        }
        path.close()
        canvas.drawPath(path, Paint(pinta).apply { style = Paint.Style.FILL })
    }


    private fun normalizarFuerza(fuerza: Float): Float {
        if (fuerza <= 0f) return 0f
        return (fuerza * 3.5f).coerceIn(24f, 110f)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detenerAnimacion()
    }
}