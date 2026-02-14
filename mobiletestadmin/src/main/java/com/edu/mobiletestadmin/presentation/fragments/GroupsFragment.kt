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
        viewModel.groupsLiveData.observe(viewLifecycleOwner) { state ->
            binding.progressLoader.root.isVisible = state is ResourceState.Loading
            binding.progressLoader.progressBarCenter.isVisible = state is ResourceState.Loading
            binding.groupsRV.isVisible = state is ResourceState.Success
            binding.emptyState.isVisible = state is ResourceState.Empty
            binding.errorState.isVisible = state is ResourceState.Error
            when (state) {
                is ResourceState.Success -> {
                    adapter.submitList(state.data)
                }
                is ResourceState.Error -> {
                    binding.errorMessage.text =
                        state.error ?: getString(R.string.error_loading_data)
                }
                else -> Unit
            }
        }

        binding.retryButton.setOnClickListener {
            viewModel.getGroups()
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
