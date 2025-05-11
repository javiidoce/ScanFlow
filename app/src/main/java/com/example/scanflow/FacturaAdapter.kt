package com.example.scanflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FacturaAdapter(private val facturas: List<File>) :
    RecyclerView.Adapter<FacturaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreArchivo: TextView = view.findViewById(R.id.nombreFactura)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_factura, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = facturas.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.nombreArchivo.text = facturas[position].name
    }
}
