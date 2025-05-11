package com.example.scanflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class FacturaPendienteAdapter(
    private val facturas: List<FacturaPendiente>,
    private val onRevisarClick: (FacturaPendiente) -> Unit
) : RecyclerView.Adapter<FacturaPendienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val archivo: TextView = view.findViewById(R.id.colArchivo)
        val proveedor: TextView = view.findViewById(R.id.colProveedor)
        val fecha: TextView = view.findViewById(R.id.colFecha)
        val btnRevisar: ImageButton = itemView.findViewById(R.id.btnRevisar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_factura_pendiente, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = facturas.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val factura = facturas[position]
        holder.archivo.text = factura.nombreArchivo
        holder.proveedor.text = factura.proveedor
        holder.fecha.text = factura.fecha
        holder.checkBox.isChecked = factura.seleccionado

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            factura.seleccionado = isChecked
        }

        holder.btnRevisar.setOnClickListener {
            onRevisarClick(factura)
        }
    }

    fun obtenerSeleccionadas(): List<FacturaPendiente> {
        return facturas.filter { it.seleccionado }
    }
}