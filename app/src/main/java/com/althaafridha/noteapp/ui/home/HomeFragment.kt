package com.althaafridha.noteapp.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.althaafridha.noteapp.R
import com.althaafridha.noteapp.data.local.entity.Notes
import com.althaafridha.noteapp.databinding.FragmentHomeBinding
import com.althaafridha.noteapp.ui.NotesViewModel
import com.althaafridha.noteapp.utils.ExtentionFunctions.setActionBar
import com.althaafridha.noteapp.utils.HelperFunction
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment(), androidx.appcompat.widget.SearchView.OnQueryTextListener {
	private var _binding: FragmentHomeBinding? = null
	private val binding get() = _binding as FragmentHomeBinding

	private val homeViewModel: NotesViewModel by viewModels()
	private val homeAdapter by lazy { HomeAdapter() }

	private var _currentData: List<Notes>? = null
	private val currentData get() = _currentData as List<Notes>

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = FragmentHomeBinding.inflate(layoutInflater)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.mHelperFunction = HelperFunction

		setHasOptionsMenu(true)

		binding.apply {
			toolbarHome.setActionBar(requireActivity())

			fabAdd.setOnClickListener {
				findNavController().navigate(R.id.action_homeFragment_to_addFragment)
			}


		}

		setupRecyclerView()
	}

	private fun setupRecyclerView() {
		binding.rvHome.apply {
			homeViewModel.getAllNote().observe(viewLifecycleOwner) {
				checkDataIsEmpty(it)
				homeAdapter.setData(it)
				_currentData = it
			}

			adapter = homeAdapter
			layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
			swipeToDelete(this)
		}
	}

	private fun checkDataIsEmpty(data: List<Notes>) {
		binding.apply {
			if (data.isEmpty()) {
				imgNoNotes.visibility = View.VISIBLE
				rvHome.visibility = View.INVISIBLE
			} else {
				imgNoNotes.visibility = View.INVISIBLE
				rvHome.visibility = View.VISIBLE
			}
		}

	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.menu_home, menu)

		val searchView = menu.findItem(R.id.menu_search)
		val actionView = searchView.actionView as? androidx.appcompat.widget.SearchView
		actionView?.setOnQueryTextListener(this)
	}

	override fun onQueryTextSubmit(query: String?): Boolean {
		query?.let {
			searchNoteByQuery(it)
		}
		return true
	}

	override fun onQueryTextChange(newText: String?): Boolean {
		newText?.let {
			searchNoteByQuery(it)
		}
		return true
	}

	private fun searchNoteByQuery(query: String) {
		val querySearch = "%$query%"
		homeViewModel.searchNotesByQuery(querySearch).observe(this) {
			homeAdapter.setData(it)
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.menu_priority_high -> homeViewModel.sortByHighPriority.observe(this) {
				homeAdapter.setData(it)
			}
			R.id.menu_priority_low -> homeViewModel.sortByLowPriority.observe(this) {
				homeAdapter.setData(it)
			}
			R.id.menu_delete -> confirmDeleteAllNote()
		}
		return super.onOptionsItemSelected(item)
	}

	private fun confirmDeleteAllNote() {

		if (currentData.isEmpty()) {
			AlertDialog.Builder(context)
				.setTitle("No Notes")
				.setMessage("There is no data to delete here")
				.setPositiveButton("Ok") { _, _ -> }
				.show()

		} else {
			AlertDialog.Builder(context)
				.setTitle("Delete All Note")
				.setMessage("Are you sure want to remove all of the notes?")
				.setPositiveButton("Yes") { _, _ ->
					homeViewModel.deleteAllData()
					Toast.makeText(context, "Successfully deleted note", Toast.LENGTH_SHORT).show()
				}
				.setNegativeButton("No") { _, _ -> }
				.show()
		}

	}

	private fun swipeToDelete(recyclerView: RecyclerView) {
		val swipeToDelete = object : ItemTouchHelper.SimpleCallback(
			0,
			ItemTouchHelper.LEFT
		) {
			override fun onMove(
				recyclerView: RecyclerView,
				viewHolder: RecyclerView.ViewHolder,
				target: RecyclerView.ViewHolder
			): Boolean {
				return false
			}

			override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
				val deletedItem = homeAdapter.listNotes[viewHolder.adapterPosition]
				homeViewModel.deleteNote(deletedItem)
				restoredData(viewHolder.itemView, deletedItem)
			}
		}

		val itemTouchHelper = ItemTouchHelper(swipeToDelete)
		itemTouchHelper.attachToRecyclerView(recyclerView)
	}

	private fun restoredData(view: View, deletedItem: Notes) {
		Snackbar.make(view, "Deleted '${deletedItem.title}'", Snackbar.LENGTH_LONG)
			.setTextColor(ContextCompat.getColor(view.context, R.color.black))
			.setAction(getString(R.string.txt_undo)) {
				homeViewModel.insertNotes(deletedItem)
			}
			.setActionTextColor(ContextCompat.getColor(view.context, R.color.black))
			.show()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

}
