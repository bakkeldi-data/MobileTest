package com.edu.mobiletestadmin.presentation.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edu.mobiletestadmin.R
import com.edu.mobiletestadmin.databinding.FragmentGroupsBinding
import com.edu.mobiletestadmin.presentation.adapter.GroupsAdapter
import com.edu.mobiletestadmin.presentation.model.GroupFragmentTypeEnum
import com.edu.mobiletestadmin.presentation.model.ResourceState
import com.edu.mobiletestadmin.presentation.model.UserGroup
import com.edu.mobiletestadmin.presentation.viewModel.GroupsViewModel
import com.edu.mobiletestadmin.utils.IImageLoader
import com.edu.mobiletestadmin.utils.addQueryChangeListener
import com.edu.mobiletestadmin.utils.viewBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class GroupsFragment : Fragment(R.layout.fragment_groups), GroupsAdapter.Listener {


    private val viewModel by viewModel<GroupsViewModel>()
    private val binding by viewBinding(FragmentGroupsBinding::bind)

    private val imageLoader by inject<IImageLoader>()

    private val adapter by lazy {
        GroupsAdapter(this, imageLoader)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getGroups()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.groupsRV.adapter = adapter

        binding.retryButton.setOnClickListener {
            viewModel.getGroups()
        }

        viewModel.groupsLiveData.observe(viewLifecycleOwner) { state ->
            val isLoading = state is ResourceState.Loading
            binding.progressLoader.root.isVisible = isLoading
            binding.progressLoader.progressBarCenter.isVisible = isLoading

            binding.errorContainer.isVisible = state is ResourceState.Error
            binding.emptyMessage.isVisible = state is ResourceState.Empty

            when (state) {
                is ResourceState.Success -> {
                    adapter.submitList(state.data)
                    binding.groupsRV.isVisible = state.data.isNotEmpty()
                }
                is ResourceState.Error -> {
                    binding.groupsRV.isVisible = false
                    binding.errorMessage.text = state.error?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.error_generic)
                }
                is ResourceState.Empty -> {
                    adapter.submitList(emptyList())
                    binding.groupsRV.isVisible = false
                }
                else -> Unit
            }
        }

        binding.searchView.addQueryChangeListener {
            if (binding.searchView.hasFocus()) {
                viewModel.searchGroups(it)
            }
        }

        binding.floatingActionButton.setOnClickListener {
            findNavController().navigate(
                GroupsFragmentDirections.fromNavGroupsToGroupFragment(
                    GroupFragmentTypeEnum.CREATE_GROUP
                )
            )
        }
    }

    override fun onGroupClick(group: UserGroup) {
        findNavController().navigate(
            GroupsFragmentDirections.fromNavGroupsToGroupFragment(
                GroupFragmentTypeEnum.EXISTING_GROUP,
                group.groupUid
            )
        )
    }

}