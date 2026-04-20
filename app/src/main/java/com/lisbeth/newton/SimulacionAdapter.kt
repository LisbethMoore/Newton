package com.lisbeth.newton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimulacionAdapter(private val lista: List<Simulacion>) :
    RecyclerView.Adapter<SimulacionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtItem: TextView = itemView.findViewById(R.id.txtItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simulacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sim = lista[position]

        holder.txtItem.text =
            "📘 Ley: ${sim.tipo}\n" +
                    "👤 Usuario: ${sim.usuario}\n" +
                    "⚖️ Masa: ${sim.masa}\n" +
                    "💪 Fuerza: ${sim.fuerza}\n" +
                    "📊 Resultado: ${sim.resultado}"
    }

    override fun getItemCount(): Int = lista.size
}