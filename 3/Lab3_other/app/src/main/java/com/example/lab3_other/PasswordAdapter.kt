package com.example.lab3_other

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lab3_other.databinding.ItemPasswordBinding
import javax.crypto.SecretKey

class PasswordAdapter(
    private val key: SecretKey,
    private val onEdit: (PasswordEntry) -> Unit,
    private val onDelete: (PasswordEntry) -> Unit
) : RecyclerView.Adapter<PasswordAdapter.ViewHolder>() {

    private var items: List<PasswordEntry> = listOf()

    fun setData(list: List<PasswordEntry>) {
        items = list
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemPasswordBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: PasswordEntry) {
            binding.tvResource.text = CryptoUtils.decrypt(entry.resource, key)
            binding.tvLogin.text = CryptoUtils.decrypt(entry.login, key)
            binding.tvPassword.text = CryptoUtils.decrypt(entry.password, key)
            binding.tvNotes.text = CryptoUtils.decrypt(entry.notes, key)

            binding.btnEdit.setOnClickListener { onEdit(entry) }
            binding.btnDelete.setOnClickListener { onDelete(entry) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPasswordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
