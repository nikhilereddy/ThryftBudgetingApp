package com.example.thryftapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu, container, false) //inflate layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //back button click
        view.findViewById<ImageView>(R.id.backButton)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val exportTransactionsBtn = view.findViewById<LinearLayout>(R.id.exportTransactions)
        exportTransactionsBtn.setOnClickListener {
            exportTransactionsToPdf() //export to pdf
        }

        view.findViewById<LinearLayout>(R.id.openCategories)?.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CategoryListFragment()) //navigate to category list
                .addToBackStack(null)
                .commit()
        }

        //add more menu click listeners if needed
    }

    private fun exportTransactionsToPdf() {
        val db = AppDatabase.getDatabase(requireContext()) //get db
        val prefs = requireContext().getSharedPreferences("thryft_session", 0) //get prefs
        val userId = prefs.getInt("user_id", -1) //get user id

        if (userId == -1) {
            Toast.makeText(requireContext(), "Invalid user", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getAllTransactions(userId) //get transactions
            if (transactions.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val pdfFile = PdfExportHelper(requireContext()).createTransactionPdf(transactions) //generate pdf

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                pdfFile
            )

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                requireContext(), 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "transaction_export_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Transaction Export",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = requireContext().getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel) //create channel
            }

            val notification = NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Transactions Statement PDF Exported")
                .setContentText("Tap to open your transactions report.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1002) //request permission
                        return@withContext
                    }
                }

                NotificationManagerCompat.from(requireContext()).notify(1001, notification) //show notification
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportTransactionsToPdf() //retry export
        }
    }
}
