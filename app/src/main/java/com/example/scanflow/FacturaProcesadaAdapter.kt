package com.example.scanflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FacturaProcesadaAdapter(
    private val facturas: List<FacturaProcesada>,
    private val onClick: (FacturaProcesada) -> Unit
) : RecyclerView.Adapter<FacturaProcesadaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val proveedor: TextView = view.findViewById(R.id.textProveedor)
        val fecha: TextView = view.findViewById(R.id.textFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_factura_procesada, parent, false)
        return ViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val factura = facturas[position]
        holder.proveedor.text = factura.proveedor
        holder.fecha.text = factura.fecha

        holder.itemView.setOnClickListener {
            onClick(factura)
        }
    }

    override fun getItemCount(): Int = facturas.size
}