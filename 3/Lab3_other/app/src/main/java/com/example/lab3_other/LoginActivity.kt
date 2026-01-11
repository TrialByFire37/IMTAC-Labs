package com.example.lab3_other

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lab3_other.databinding.ActivityLoginBinding
import javax.crypto.SecretKey
import android.widget.CompoundButton

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var db: PassDatabase
    private lateinit var currentKey: SecretKey
    private var masterCode: String = "" // текущий мастер-пароль

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = PassDatabase.getDatabase(this)

        binding.btnLogin.setOnClickListener {
            val enteredMaster = binding.editMaster.text.toString()
            if (enteredMaster.isEmpty()) {
                Toast.makeText(this, "Введите мастер-пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentKey = CryptoUtils.generateKey(enteredMaster)

            val list = db.passDao().getAll()
            if (list.isNotEmpty()) {
                try {
                    CryptoUtils.decrypt(list[0].resource, currentKey)
                } catch (e: Exception) {
                    Toast.makeText(this, "Неверный мастер-пароль", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("master_key", enteredMaster)
            startActivity(intent)
            finish()
        }

        binding.chkShowMaster.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                binding.editMaster.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.editMaster.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.editMaster.setSelection(binding.editMaster.text.length)
        }


        binding.btnChangeMaster.setOnClickListener {
            val oldMaster = binding.editOldMaster.text.toString()
            val newMaster = binding.editNewMaster.text.toString()

            if (oldMaster.isEmpty() || newMaster.isEmpty()) {
                Toast.makeText(this, "Введите старый и новый мастер-пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val oldKey = CryptoUtils.generateKey(oldMaster)
            val newKey = CryptoUtils.generateKey(newMaster)

            val list = db.passDao().getAll()
            if (list.isEmpty()) {
                // база пустая
                Toast.makeText(this, "База пуста, пароль установлен", Toast.LENGTH_SHORT).show()
                currentKey = newKey
                masterCode = newMaster
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("master_key", newMaster)
                startActivity(intent)
                finish()
                return@setOnClickListener
            }

            // проверка старого пароля
            var validOldPassword = false
            for (entry in list) {
                try {
                    CryptoUtils.decrypt(entry.resource, oldKey)
                    validOldPassword = true
                    break
                } catch (e: Exception) {
                    // игнорируем ошибки отдельных записей
                }
            }

            if (!validOldPassword) {
                Toast.makeText(this, "Старый мастер-пароль неверный", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // перешифровка всех данных новым ключом
            list.forEach { entry ->
                try {
                    val decryptedResource = CryptoUtils.decrypt(entry.resource, oldKey)
                    val decryptedLogin = CryptoUtils.decrypt(entry.login, oldKey)
                    val decryptedPassword = CryptoUtils.decrypt(entry.password, oldKey)
                    val decryptedNotes = CryptoUtils.decrypt(entry.notes, oldKey)

                    val updatedEntry = entry.copy(
                        resource = CryptoUtils.encrypt(decryptedResource, newKey),
                        login = CryptoUtils.encrypt(decryptedLogin, newKey),
                        password = CryptoUtils.encrypt(decryptedPassword, newKey),
                        notes = CryptoUtils.encrypt(decryptedNotes, newKey)
                    )
                    db.passDao().update(updatedEntry)
                } catch (e: Exception) { e.printStackTrace() }
            }

            currentKey = newKey
            masterCode = newMaster
            Toast.makeText(this, "Мастер-пароль успешно изменён", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("master_key", newMaster)
            startActivity(intent)
            finish()
        }

    }

    private fun changeMasterPassword(oldKey: SecretKey, newPassword: String) {
        val newKey = CryptoUtils.generateKey(newPassword)
        val list = db.passDao().getAll()

        list.forEach { entry ->
            try {
                val decryptedResource = CryptoUtils.decrypt(entry.resource, oldKey)
                val decryptedLogin = CryptoUtils.decrypt(entry.login, oldKey)
                val decryptedPassword = CryptoUtils.decrypt(entry.password, oldKey)
                val decryptedNotes = CryptoUtils.decrypt(entry.notes, oldKey)

                val updatedEntry = entry.copy(
                    resource = CryptoUtils.encrypt(decryptedResource, newKey),
                    login = CryptoUtils.encrypt(decryptedLogin, newKey),
                    password = CryptoUtils.encrypt(decryptedPassword, newKey),
                    notes = CryptoUtils.encrypt(decryptedNotes, newKey)
                )

                db.passDao().update(updatedEntry)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
