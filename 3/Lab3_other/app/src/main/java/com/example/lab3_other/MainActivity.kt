package com.example.lab3_other

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab3_other.databinding.ActivityMainBinding
import com.example.lab3_other.databinding.DialogEditBinding
import javax.crypto.SecretKey

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: PassDatabase
    private lateinit var key: SecretKey
    private lateinit var masterCode: String
    private lateinit var adapter: PasswordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = PassDatabase.getDatabase(this)
        masterCode = intent.getStringExtra("master_key") ?: ""
        key = CryptoUtils.generateKey(masterCode)

        adapter = PasswordAdapter(key,
            onEdit = { entry -> showEditDialog(entry) },
            onDelete = { entry ->
                db.passDao().delete(entry)
                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
                loadData()
            }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.btnAdd.setOnClickListener {
            val encrypted = PasswordEntry(
                resource = CryptoUtils.encrypt(binding.editResource.text.toString(), key),
                login = CryptoUtils.encrypt(binding.editLogin.text.toString(), key),
                password = CryptoUtils.encrypt(binding.editPassword.text.toString(), key),
                notes = CryptoUtils.encrypt(binding.editNotes.text.toString(), key)
            )
            db.passDao().insert(encrypted)
            loadData()
        }

        binding.btnExport.setOnClickListener { exportData("exported_passwords.json") }
        binding.btnImport.setOnClickListener { importData("exported_passwords.json") }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        loadData()
    }

    private fun loadData() {
        val list = db.passDao().getAll()
        adapter.setData(list)
    }

    private fun showEditDialog(entry: PasswordEntry) {
        val dialogBinding = DialogEditBinding.inflate(layoutInflater)
        dialogBinding.editResource.setText(CryptoUtils.decrypt(entry.resource, key))
        dialogBinding.editLogin.setText(CryptoUtils.decrypt(entry.login, key))
        dialogBinding.editPassword.setText(CryptoUtils.decrypt(entry.password, key))
        dialogBinding.editNotes.setText(CryptoUtils.decrypt(entry.notes, key))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Редактировать запись")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val updated = entry.copy(
                    resource = CryptoUtils.encrypt(dialogBinding.editResource.text.toString(), key),
                    login = CryptoUtils.encrypt(dialogBinding.editLogin.text.toString(), key),
                    password = CryptoUtils.encrypt(dialogBinding.editPassword.text.toString(), key),
                    notes = CryptoUtils.encrypt(dialogBinding.editNotes.text.toString(), key)
                )
                db.passDao().update(updated)
                loadData()
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun exportData(filename: String) {
        val list = db.passDao().getAll()
        val gson = com.google.gson.Gson()
        val json = gson.toJson(list)

        val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val file = java.io.File(downloads, filename)
        file.writeText(json)

        Toast.makeText(this, "Данные экспортированы в ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }

    private fun importData(filename: String) {
        val input = android.widget.EditText(this)
        input.hint = "Старый мастер-пароль"
        input.isSingleLine = true

        AlertDialog.Builder(this)
            .setTitle("Введите пароль экспорта")
            .setMessage("Укажите мастер-пароль, который использовался при экспорте данных.")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val oldPassword = input.text.toString()
                if (oldPassword.isEmpty()) {
                    Toast.makeText(this, "Пароль не может быть пустым", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                importWithPassword(filename, oldPassword)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun importWithPassword(filename: String, oldPassword: String) {
        try {
            val downloads = android.os.Environment
                .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)

            val file = java.io.File(downloads, filename)
            if (!file.exists()) {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
                return
            }

            val oldKey = CryptoUtils.generateKey(oldPassword)
            val newKey = key

            val gson = com.google.gson.Gson()
            val type = com.google.gson.reflect.TypeToken
                .getParameterized(List::class.java, PasswordEntry::class.java).type

            val importedList: List<PasswordEntry> = gson.fromJson(file.readText(), type)

            importedList.forEach { entry ->
                try {
                    val decryptedResource = CryptoUtils.decrypt(entry.resource, oldKey)
                    val decryptedLogin = CryptoUtils.decrypt(entry.login, oldKey)
                    val decryptedPassword = CryptoUtils.decrypt(entry.password, oldKey)
                    val decryptedNotes = CryptoUtils.decrypt(entry.notes, oldKey)

                    val newEntry = PasswordEntry(
                        id = 0,
                        resource = CryptoUtils.encrypt(decryptedResource, newKey),
                        login = CryptoUtils.encrypt(decryptedLogin, newKey),
                        password = CryptoUtils.encrypt(decryptedPassword, newKey),
                        notes = CryptoUtils.encrypt(decryptedNotes, newKey)
                    )

                    db.passDao().insert(newEntry)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Toast.makeText(this, "Данные успешно импортированы", Toast.LENGTH_SHORT).show()
            loadData()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

}
