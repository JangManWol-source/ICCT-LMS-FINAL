package com.icct.icctlms.newsAndUpdates

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.icct.icctlms.Authentication.Teacher.TeacherMainActivity
import com.icct.icctlms.R
import com.icct.icctlms.adapter.AnnouncementAdapter
import com.icct.icctlms.data.AnnouncementData
import kotlinx.android.synthetic.main.activity_create_nesws_and_updates.*
import kotlinx.android.synthetic.main.activity_forgot_password.*
import kotlinx.android.synthetic.main.announcement_item.*
import kotlinx.android.synthetic.main.announcement_update_layout.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.collections.ArrayList

class CreateNewsAndUpdates : AppCompatActivity() {
    private lateinit var database : DatabaseReference
    private lateinit var uid : String
    private lateinit var recyclerView: RecyclerView
    private lateinit var announcementArrayList: ArrayList<AnnouncementData>
    private lateinit var name : String
    private lateinit var date : String
    private lateinit var title : String
    private lateinit var today : Calendar
    private lateinit var description : String
    private lateinit var dialog: Dialog
    private lateinit var updateLayout : View
    private lateinit var updateEditText: EditText
    private lateinit var updateTitle : EditText
    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_nesws_and_updates)
        database = FirebaseDatabase.getInstance().getReference("Admin").child("Announcements")
        uid = randomCode()
        name = "Admin na walang Jowa"



        today = Calendar.getInstance()
        val day = today.get(Calendar.DAY_OF_MONTH)
        val monthList = arrayOf("January", "February",
            "March", "April", "May", "June", "July",
            "August", "September", "October", "November",
            "December")
        val month = monthList[today.get(Calendar.MONTH)].uppercase()
        val trimMonth = month.subSequence(0, 3)
        date = "$day\n$trimMonth"

        submitAnnounce()
        
        recyclerView = findViewById(R.id.recyclerView_announcement)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this.applicationContext)
        announcementArrayList = arrayListOf()

        val layoutInflater = layoutInflater
        updateLayout = layoutInflater.inflate(R.layout.announcement_update_layout, null)
        updateEditText = updateLayout.findViewById(R.id.update_et)
        updateTitle = updateLayout.findViewById(R.id.update_et_title)


        executeAnnouncement()
    }

    private fun submitAnnounce() {

        submit_ann_btn.setOnClickListener{
            progressDialogShow()
            val announcementID = randomCode()
            val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime.now()
            } else {
                TODO("VERSION.SDK_INT < O")
            }
            val sortKey = now.toMillis().toString()
            title = et_title.text.toString()
            description = et_desc.text.toString()
            val data = AnnouncementData(name, uid, title, description, date, announcementID, sortKey)
val announce = FirebaseDatabase.getInstance().getReference("Admin").child("Announcements").child(announcementID)
            announce.setValue(data).addOnSuccessListener {
                progressDialogHide()
                MaterialAlertDialogBuilder(this)
                    .setMessage("Successfully posted! Create another announcement?")
                    .setPositiveButton("OKAY"){_,_ ->
                        et_title.text?.clear()
                        et_desc.text?.clear()
                        executeAnnouncement()
                    }
                    .setNegativeButton("CANCEL"){_,_ ->
                        startActivity(Intent(this, TeacherMainActivity::class.java))
                        finish()
                    }.show()
            }
        }
    }

    private fun progressDialogShow(){
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_layout)
        dialog.setTitle("Loading please wait")
        dialog.setCancelable(false)
        dialog.show()
    }
    private fun progressDialogHide(){
        dialog.hide()
    }

    private fun executeAnnouncement() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    announcementArrayList.clear()


                    for (postSnapShot in snapshot.children){
                        snapshot.child(uid)
                        val announcement = postSnapShot.getValue(AnnouncementData::class.java)
                        announcementArrayList.add(announcement!!)
                    }
                    val adapter = AnnouncementAdapter(announcementArrayList)
                    announcementArrayList.sortByDescending {
                        it.sortKey
                    }
                    recyclerView.adapter = adapter

                    //adapter click listener
                    adapter.setOnItemClickListener(object : AnnouncementAdapter.onItemClickListener{
                        override fun onItemClick(position: Int) {
                            val message = announcementArrayList[position].description
                            val annID = announcementArrayList[position].announcementID.toString()
                            title = announcementArrayList[position].title.toString()
                            val reference = FirebaseDatabase.getInstance().getReference("Admin").child("Announcements")
                            updateEditText.setText(message)
                            updateTitle.setText(title)

                            MaterialAlertDialogBuilder(this@CreateNewsAndUpdates)
                                .setMessage("Announcement: $message")
                                .setPositiveButton("UPDATE"){_,_ ->
                                  MaterialAlertDialogBuilder(this@CreateNewsAndUpdates)
                                   .setTitle("Update Announcement")
                                    .setView(updateLayout)
                                    .setOnCancelListener{
                                            hideLayout()
                                        }
                                    .setPositiveButton("UPDATE"){_, _ ->
                                        val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            LocalDateTime.now()
                                        } else {
                                            TODO("VERSION.SDK_INT < O")
                                        }
                                        val sortKey = now.toMillis().toString()
                                        title = updateTitle.text.toString()
                                        description = updateEditText.text.toString()
                                        val data = AnnouncementData(name, uid, title, description, date, annID, sortKey)
                                        reference.child(annID).setValue(data).addOnSuccessListener {
                                            Toast.makeText(this@CreateNewsAndUpdates, "Successfully Updated", Toast.LENGTH_SHORT).show()
                                            executeAnnouncement()
                                            hideLayout()
                                    }


                                    }.show()

                                }
                                .setNegativeButton("CANCEL"){_,_ ->

                                }
                                .setNeutralButton("DELETE"){_,_ ->
                                    progressDialogShow()
                                    reference.child(annID.toString()).removeValue().addOnSuccessListener {
                                        progressDialogHide()
                                        Toast.makeText(this@CreateNewsAndUpdates, "Deleted successfully!", Toast.LENGTH_SHORT).show()
                                        adapter.deleteItem(position)
                                        recyclerView.adapter?.notifyItemRemoved(position)
                                    executeAnnouncement()

                                    }
                                }.show()
                        }

                    })

                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }



    private fun hideLayout() {
        val parent : ViewGroup = updateLayout.parent as ViewGroup
        parent.removeView(updateLayout)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun LocalDateTime.toMillis(zone: ZoneId = ZoneId.systemDefault()) = atZone(zone)?.toInstant()?.toEpochMilli()
    private fun randomCode(): String = List(6) {
        (('a'..'z') + ('A'..'Z') + ('0'..'9')).random()
    }.joinToString("")
}