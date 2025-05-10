package com.example.buttonmashers

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText

class ProfileActivity : AppCompatActivity() {

    lateinit var dbHelper: GameDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup DB helper.
        dbHelper = GameDatabaseHelper(
            this,
            { fileName -> resources.getIdentifier(fileName, "drawable", packageName) }
        )

        // Initialize views
        val nameTextView = findViewById<TextView>(R.id.nameTextView)
        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val profilePic = findViewById<ImageView>(R.id.profileImageView)
        val changeProfilePicButton = findViewById<FloatingActionButton>(R.id.changeProfilePicButton)
        val editProfileButton = findViewById<Button>(R.id.editProfileButton)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = findViewById(R.id.viewPager)

        // Set default profile info
        nameTextView.text = dbHelper.getProfile().name
        emailTextView.text = dbHelper.getProfile().email

        val profileImageBase64 = dbHelper.getProfile().imagePath
        if (profileImageBase64 != null) {
            val decodedBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            profilePic.setImageBitmap(bitmap)
        } else {
            profilePic.setImageResource(R.drawable.user)
        }

        // Handle edit profile button click

        editProfileButton.setOnClickListener {
            val dialog = UserEditDialogFragment()
            val bundle = Bundle()
            bundle.putString("name", nameTextView.text.toString())
            bundle.putString("email", emailTextView.text.toString())
            dialog.arguments = bundle
            dialog.show(supportFragmentManager, "profileDialog")
        }

        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->if (uri != null) {
            profilePic.setImageURI(uri)

            val inputStream = contentResolver.openInputStream(uri)
            val imageBytes = inputStream?.readBytes()
            val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)
            inputStream?.close()

            dbHelper.updateProfileImage(base64Image.toString())
        }
        }

        changeProfilePicButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Setup Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // Setup ViewPager and TabLayout
        val adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter
        tabLayout.addTab(tabLayout.newTab().setText("Owned Titles"))
        tabLayout.addTab(tabLayout.newTab().setText("Order History"))
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Owned Titles" else "Order History"
        }.attach()

        supportFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            val updatedName = bundle.getString("name")
            val updatedEmail = bundle.getString("email")

            // Update the TextViews with the new data
            nameTextView.text = updatedName
            emailTextView.text = updatedEmail

            dbHelper.updateProfile(nameTextView.text.toString(), emailTextView.text.toString())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

class GameTitleAdapter(private val gameTitles: List<Game>, val dbHelper: GameDatabaseHelper) : RecyclerView.Adapter<GameTitleAdapter.GameTitleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameTitleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_owned_title, parent, false)
        return GameTitleViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameTitleViewHolder, position: Int) {
        val gameTitle = gameTitles[position]
        holder.gameTitle.text = gameTitle.title
        holder.hoursPlayed.text = "Hours Played: ${gameTitle.hoursPlayed}"
        holder.gameRating.rating = gameTitle.rating
        holder.gameImage.setImageResource(gameTitle.imageResId)
        holder.gameRating.setOnRatingBarChangeListener { _, rating, _ ->
            dbHelper.updateRating(gameTitle.id, rating)
        }
    }

    override fun getItemCount(): Int = gameTitles.size

    class GameTitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gameImage: ImageView = itemView.findViewById(R.id.game_image)
        val gameTitle: TextView = itemView.findViewById(R.id.game_title)
        val hoursPlayed: TextView = itemView.findViewById(R.id.hours_played)
        val gameRating: RatingBar = itemView.findViewById(R.id.game_rating)
    }
}

class UserEditDialogFragment : DialogFragment(R.layout.user_edit_fragment) {
    private lateinit var editTextName: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_edit_fragment, container, false)

        editTextName = view.findViewById(R.id.editTextName)
        editTextEmail = view.findViewById(R.id.editTextEmail)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonCancel = view.findViewById(R.id.buttonCancel)

        //grab values of name and email from profile activity
        val name = arguments?.getString("name")
        val email = arguments?.getString("email")
        editTextName.setText(name)
        editTextEmail.setText(email)

        buttonSave.isEnabled = editTextName.text.toString().isNotEmpty() && editTextEmail.text.toString().isNotEmpty()

        editTextName.addTextChangedListener {
            buttonSave.isEnabled = editTextName.text.toString().isNotEmpty() && editTextEmail.text.toString().isNotEmpty()
        }

        editTextEmail.addTextChangedListener {
            buttonSave.isEnabled = editTextName.text.toString().isNotEmpty() && editTextEmail.text.toString().isNotEmpty()
        }

        buttonCancel.setOnClickListener {
            dismiss()  // Close the dialog on cancel
        }

        buttonSave.setOnClickListener {
            val name = editTextName.text.toString()
            val email = editTextEmail.text.toString()
            val result = Bundle().apply {
                putString("name", name)
                putString("email", email)
            }
            setFragmentResult("requestKey", result)
            dismiss()
        }

        return view
    }
}

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OwnedTitlesFragment()
            1 -> OrderHistoryFragment()
            else -> OwnedTitlesFragment()
        }
    }
}