package com.vinzdits.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.vinzdits.gallery.adapter.PhotoAdapter
import com.vinzdits.gallery.databinding.FragmentGalleryBinding

/** Tab "Foto": grid 3 kolom semua foto, terbaru dulu. */
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PhotoViewModel by activityViewModels()
    private lateinit var adapter: PhotoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PhotoAdapter { photo, position ->
            ViewerActivity.start(requireContext(), ViewerActivity.MODE_ALL, null, position)
        }
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter

        viewModel.photos.observe(viewLifecycleOwner) { photos ->
            adapter.submitList(photos)
            binding.emptyView.isVisible = photos.isEmpty()
            binding.progressBar.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Segarkan setelah kembali dari viewer (mis. habis hapus/favorit).
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
