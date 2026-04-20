package com.lisbeth.newton

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.lisbeth.newton.databinding.ActivityPrimeraLeyBinding


class PrimeraLey : AppCompatActivity() {

    data class ResultadoAnalisis(
        val estado: String,
        val comportamiento: String,
        val peso: Double,
        val normal: Double,
        val friccionMaxima: Double,
        val fuerzaNeta: Double,
        val aceleracion: Double,          // a = ΣF / m
        val tipoEstado: SimulacionPrimeraLeyView.EstadoSimulacion,
        val explicacion: String
    )

    private lateinit var binding: ActivityPrimeraLeyBinding
    private var explicacionActual: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrimeraLeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarEstadoInicial()

        binding.btnAnalizar.setOnClickListener { analizarComportamiento() }
        binding.btnExplicacion.setOnClickListener { mostrarOcultarExplicacion() }
    }

    //estado inicial

    private fun configurarEstadoInicial() {
        binding.vistaSimulacion.reiniciar()
        binding.btnExplicacion.visibility = View.GONE
        binding.cardExplicacion.visibility = View.GONE
    }

    //analizar comportamiento

    private fun analizarComportamiento() {

        //validar entradas
        val masa = leerDouble(binding.etMasa.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para la masa"); return
        }
        val fuerzaAplicada = leerDouble(binding.etFuerzaAplicada.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para la fuerza aplicada"); return
        }
        val coeficiente = leerDouble(binding.etCoeficienteFriccion.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para el coeficiente de fricción"); return
        }
        val velocidadInicial = leerDouble(binding.etVelocidadInicial.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para la velocidad inicial"); return
        }

        if (masa <= 0.0)           { mostrarError("La masa debe ser mayor que cero"); return }
        if (coeficiente < 0.0)     { mostrarError("El coeficiente de fricción no puede ser negativo"); return }
        if (velocidadInicial < 0.0) { mostrarError("La velocidad inicial no puede ser negativa"); return }

        //calculo de magnitudes
        val g              = 9.8
        val peso           = masa * g
        val normal         = peso
        val friccionMaxima = coeficiente * normal
        val fuerzaNeta: Double
        val aceleracion: Double

        //resultado
        val resultado: ResultadoAnalisis = when {

            //caso sin friccion
            fuerzaAplicada == 0.0 && velocidadInicial > 0.0 && coeficiente == 0.0 -> {
                fuerzaNeta  = 0.0
                aceleracion = 0.0
                ResultadoAnalisis(
                    estado         = "Equilibrio dinámico ideal",
                    comportamiento = "El objeto se mueve con velocidad constante (sin fricción)",
                    peso           = peso,
                    normal         = normal,
                    friccionMaxima = friccionMaxima,
                    fuerzaNeta     = fuerzaNeta,
                    aceleracion    = aceleracion,
                    tipoEstado     = SimulacionPrimeraLeyView.EstadoSimulacion.EQUILIBRIO_DINAMICO,
                    explicacion    =
                        "El objeto se mueve a ${velocidadInicial} m/s y no existe ninguna fuerza que " +
                                "se oponga a ese movimiento: la fuerza aplicada es 0 N y el coeficiente de " +
                                "fricción también es 0. Por lo tanto la fuerza neta es 0 N y no hay aceleración. " +
                                "El objeto continuará moviéndose indefinidamente a velocidad constante. " +
                                "Este es el caso ideal de la Primera Ley de Newton: la inercia pura."
                )
            }

            // caso uno reposo
            fuerzaAplicada <= friccionMaxima && velocidadInicial == 0.0 -> {
                fuerzaNeta  = 0.0
                aceleracion = 0.0
                ResultadoAnalisis(
                    estado         = "Reposo",
                    comportamiento = "El objeto permanece quieto",
                    peso           = peso,
                    normal         = normal,
                    friccionMaxima = friccionMaxima,
                    fuerzaNeta     = fuerzaNeta,
                    aceleracion    = aceleracion,
                    tipoEstado     = SimulacionPrimeraLeyView.EstadoSimulacion.REPOSO,
                    explicacion    =
                        "Se aplicó una fuerza de ${"%.2f".format(fuerzaAplicada)} N, pero la " +
                                "fricción máxima del suelo es ${"%.2f".format(friccionMaxima)} N. " +
                                "Como la fuerza aplicada NO supera la fricción, el suelo \"aguanta\" " +
                                "el empuje y el objeto no se mueve. La fuerza neta es 0 N y la " +
                                "aceleración es 0 m/s². Esto confirma la Primera Ley: un objeto en " +
                                "reposo permanece en reposo si la fuerza neta es cero."
                )
            }

            //caso dos equilibrio dinamico
            fuerzaAplicada == friccionMaxima && velocidadInicial > 0.0 -> {
                fuerzaNeta  = 0.0
                aceleracion = 0.0
                ResultadoAnalisis(
                    estado         = "Equilibrio dinámico",
                    comportamiento = "El objeto se mueve con velocidad constante",
                    peso           = peso,
                    normal         = normal,
                    friccionMaxima = friccionMaxima,
                    fuerzaNeta     = fuerzaNeta,
                    aceleracion    = aceleracion,
                    tipoEstado     = SimulacionPrimeraLeyView.EstadoSimulacion.EQUILIBRIO_DINAMICO,
                    explicacion    =
                        "El objeto ya tenía una velocidad de ${"%.2f".format(velocidadInicial)} m/s. " +
                                "La fuerza aplicada (${"%.2f".format(fuerzaAplicada)} N) es exactamente " +
                                "igual a la fricción (${"%.2f".format(friccionMaxima)} N), por lo que se " +
                                "anulan mutuamente. La fuerza neta es 0 N y la aceleración es 0 m/s². " +
                                "El objeto mantiene su velocidad sin cambio. Esto es la Primera Ley en " +
                                "movimiento: si la fuerza neta es cero, la velocidad no cambia."
                )
            }

            // caso tres, equilibrio roto
            fuerzaAplicada > friccionMaxima -> {
                fuerzaNeta  = fuerzaAplicada - friccionMaxima
                aceleracion = fuerzaNeta / masa
                ResultadoAnalisis(
                    estado         = "El equilibrio se rompió",
                    comportamiento = "El objeto comienza a acelerar",
                    peso           = peso,
                    normal         = normal,
                    friccionMaxima = friccionMaxima,
                    fuerzaNeta     = fuerzaNeta,
                    aceleracion    = aceleracion,
                    tipoEstado     = SimulacionPrimeraLeyView.EstadoSimulacion.EQUILIBRIO_ROTO,
                    explicacion    =
                        "La fuerza aplicada (${"%.2f".format(fuerzaAplicada)} N) supera la " +
                                "fricción máxima (${"%.2f".format(friccionMaxima)} N). Existe una fuerza " +
                                "neta de ${"%.2f".format(fuerzaNeta)} N hacia la derecha. Con una masa de " +
                                "${"%.2f".format(masa)} kg, la aceleración resultante es " +
                                "${"%.2f".format(aceleracion)} m/s². El equilibrio se rompió: el objeto " +
                                "ya no mantiene su estado y empieza a ganar velocidad. La Primera Ley " +
                                "dice que esto ocurre solo cuando hay una fuerza neta distinta de cero."
                )
            }

            // caso cuatro, desaceleracion
            else -> {
                fuerzaNeta  = fuerzaAplicada - friccionMaxima   // valor negativo
                aceleracion = fuerzaNeta / masa                  // negativa frena
                ResultadoAnalisis(
                    estado         = "Desaceleración",
                    comportamiento = "El objeto se está frenando",
                    peso           = peso,
                    normal         = normal,
                    friccionMaxima = friccionMaxima,
                    fuerzaNeta     = fuerzaNeta,
                    aceleracion    = aceleracion,
                    tipoEstado     = SimulacionPrimeraLeyView.EstadoSimulacion.DESACELERACION,
                    explicacion    =
                        "El objeto se movía a ${"%.2f".format(velocidadInicial)} m/s, pero la " +
                                "fricción (${"%.2f".format(friccionMaxima)} N) supera la fuerza aplicada " +
                                "(${"%.2f".format(fuerzaAplicada)} N). La fuerza neta es " +
                                "${"%.2f".format(fuerzaNeta)} N (negativa, actúa en contra del movimiento). " +
                                "Esto produce una aceleración de ${"%.2f".format(aceleracion)} m/s² que va " +
                                "reduciendo la velocidad hasta cero. La inercia del objeto mantiene el " +
                                "movimiento por un tiempo, pero la fricción termina deteniéndolo."
                )
            }
        }

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("simulaciones")

        val usuario = FirebaseAuth.getInstance().currentUser?.email ?: "anonimo"

        val simulacion = Simulacion(
            usuario,
            masa,
            fuerzaAplicada,
            resultado.estado,
            "Primera Ley"
        )

        ref.push().setValue(simulacion)

        //resultados
        mostrarResultados(resultado)

        binding.vistaSimulacion.configurarSimulacion(
            estado             = resultado.tipoEstado,
            texto              = resultado.estado,
            fuerzaAplicada     = fuerzaAplicada.toFloat(),
            friccionMaxima     = friccionMaxima.toFloat(),
            fuerzaNeta         = resultado.fuerzaNeta.toFloat(),
            aceleracion        = resultado.aceleracion.toFloat(),
            masa               = masa.toFloat(),
            velocidadInicialMs = velocidadInicial.toFloat()
        )

        //guardar y mostrar boton

        explicacionActual = resultado.explicacion
        binding.btnExplicacion.visibility  = View.VISIBLE
        binding.cardExplicacion.visibility = View.GONE
        binding.btnExplicacion.text        = "Ver explicación"
    }

    //muestra resultados

    private fun mostrarResultados(r: ResultadoAnalisis) {
        binding.tvEstado.text         = "Estado: ${r.estado}"
        binding.tvComportamiento.text = "Comportamiento: ${r.comportamiento}"
        binding.tvPeso.text           = "Peso (W): ${"%.2f".format(r.peso)} N"
        binding.tvNormal.text         = "Normal (N): ${"%.2f".format(r.normal)} N"
        binding.tvFriccion.text       = "Fricción máxima (f): ${"%.2f".format(r.friccionMaxima)} N"
        binding.tvFuerzaNeta.text     = "Fuerza neta (ΣF): ${"%.2f".format(r.fuerzaNeta)} N"
    }

    //ver o ocultar explicacipn

    private fun mostrarOcultarExplicacion() {
        if (explicacionActual == null) return
        if (binding.cardExplicacion.visibility == View.GONE) {
            binding.tvExplicacion.text     = explicacionActual
            binding.cardExplicacion.visibility = View.VISIBLE
            binding.btnExplicacion.text    = "Ocultar explicación"
        } else {
            binding.cardExplicacion.visibility = View.GONE
            binding.btnExplicacion.text    = "Ver explicación"
        }
    }

    //utilidades

    private fun leerDouble(texto: String?): Double? {
        if (texto.isNullOrBlank()) return null
        return texto.trim().toDoubleOrNull()
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}