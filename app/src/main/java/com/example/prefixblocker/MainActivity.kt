package com.example.prefixblocker

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private val requestRoleCode = 1

    private lateinit var statusText: TextView
    private lateinit var enableButton: Button
    private lateinit var blockSwitch: Switch
    private lateinit var historyCount: TextView
    private lateinit var historyText: TextView
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        enableButton = findViewById(R.id.enableButton)
        blockSwitch = findViewById(R.id.blockSwitch)
        historyCount = findViewById(R.id.historyCount)
        historyText = findViewById(R.id.historyText)
        clearButton = findViewById(R.id.clearButton)
        val input = findViewById<EditText>(R.id.prefixInput)

        input.setText(PrefixStore.load(this).joinToString("\n"))

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val prefixes = input.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            PrefixStore.save(this, prefixes)
            Toast.makeText(this, getString(R.string.saved, prefixes.size), Toast.LENGTH_SHORT).show()
        }

        enableButton.setOnClickListener { requestScreeningRole() }

        blockSwitch.isChecked = PrefixStore.isEnabled(this)
        blockSwitch.setOnCheckedChangeListener { _, checked ->
            PrefixStore.setEnabled(this, checked)
            updateStatus()
        }

        clearButton.setOnClickListener {
            PrefixStore.clearLog(this)
            updateHistory()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateHistory()
    }

    private fun roleManager(): RoleManager = getSystemService(RoleManager::class.java)

    private fun hasRole(): Boolean = roleManager().isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    private fun requestScreeningRole() {
        val rm = roleManager()
        if (!rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, R.string.role_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        if (!hasRole()) {
            startActivityForResult(
                rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                requestRoleCode
            )
        }
    }

    @Deprecated("Framework Activity API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updateStatus()
    }

    private fun updateStatus() {
        val held = hasRole()
        enableButton.visibility = if (held) View.GONE else View.VISIBLE
        statusText.setText(
            when {
                !held -> R.string.status_no_role
                PrefixStore.isEnabled(this) -> R.string.status_on
                else -> R.string.status_paused
            }
        )
        statusText.setTextColor(
            getColor(if (held && PrefixStore.isEnabled(this)) R.color.ok else R.color.danger)
        )
    }

    private fun updateHistory() {
        val log = PrefixStore.loadLog(this)
        historyCount.text = getString(R.string.history_count, log.size)
        clearButton.isEnabled = log.isNotEmpty()
        clearButton.alpha = if (log.isEmpty()) 0.4f else 1f

        val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        historyText.text =
            if (log.isEmpty()) getString(R.string.history_empty)
            else log.joinToString("\n") { "${it.number}    ${fmt.format(Date(it.time))}" }
    }
}
