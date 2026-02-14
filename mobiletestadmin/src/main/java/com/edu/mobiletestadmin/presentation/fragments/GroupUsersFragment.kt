package com.edu.mobiletestadmin.presentation.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.edu.mobiletestadmin.R
import com.edu.mobiletestadmin.databinding.FragmentGroupUsersBinding
import com.edu.mobiletestadmin.presentation.adapter.UsersAdapter
import com.edu.mobiletestadmin.presentation.adapter.UsersPagerAdapter
import com.edu.mobiletestadmin.presentation.model.ResourceState
import com.edu.mobiletestadmin.presentation.model.User
import com.edu.mobiletestadmin.presentation.model.UserTypeEnum
import com.edu.mobiletestadmin.presentation.viewModel.GroupViewModel
import com.edu.mobiletestadmin.utils.IImageLoader
import com.edu.mobiletestadmin.utils.showToast
import com.edu.mobiletestadmin.utils.viewBinding
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class GroupUsersFragment : Fragment(R.layout.fragment_group_users), UsersAdapter.Listener,
    UsersPagerAdapter.SelectionListener {

    private val viewModel by viewModel<GroupViewModel>()

    private val binding by viewBinding(FragmentGroupUsersBinding::bind)

    private val imageLoader: IImageLoader by inject()

    private val args by navArgs<GroupUsersFragmentArgs>()

    private val studentsAdapter by lazy {
        UsersAdapter(this, imageLoader)
    }

    private val teachersAdapter by lazy {
        UsersAdapter(this, imageLoader)
    }

    private val viewPagerAdapter by lazy {
        UsersPagerAdapter(
            studentsAdapter,
            teachersAdapter,
            selectionListener = this,
            noStudentsMessage = R.string.no_students_in_the_group,
            noTeachersMessage = R.string.no_teachers_in_the_group
        )
    }

    private var usersBottomSheet: UsersBottomSheetFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getGroupStudents(args.groupUid)
        viewModel.getGroupTeachers(args.groupUid)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = args.groupName

        binding.toolbar.setupWithNavController(findNavController())

        binding.viewPager.adapter = viewPagerAdapter


        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (UserTypeEnum.getTypeByPosition(position)) {
                UserTypeEnum.STUDENT -> resources.getString(R.string.students)
                UserTypeEnum.TEACHER -> resources.getString(R.string.teachers)
            }
        }.attach()

        viewModel.apply {
            viewModel.groupStudents.observe(viewLifecycleOwner) { state ->

                when (state) {
                    is ResourceState.Success -> {
                        viewPagerAdapter.updateEmptyState(
                            if (state.data.isEmpty()) UsersPagerAdapter.PageDataState.EMPTY else UsersPagerAdapter.PageDataState.SUCCESS,
                            UserTypeEnum.STUDENT
                        )
                        studentsAdapter.submitList(state.data)
                    }
                    is ResourceState.Error -> {
                        showToast(state.error)
                    }
                    else -> Unit
                }
            }

            viewModel.groupTeachers.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is ResourceState.Success -> {
                        viewPagerAdapter.updateEmptyState(
                            if (state.data.isEmpty()) UsersPagerAdapter.PageDataState.EMPTY else UsersPagerAdapter.PageDataState.SUCCESS,
                            UserTypeEnum.TEACHER
                        )
                        teachersAdapter.submitList(state.data)
                    }
                    is ResourceState.Error -> {
                        showToast(state.error)
                    }
                    else -> Unit
                }
            }

            loading.observe(viewLifecycleOwner) {
                binding.loader.root.isVisible = it
                binding.loader.progressBarCenter.isVisible = it
            }

        }

        binding.floatingActionButton.setOnClickListener {
            if (viewPagerAdapter.getStudentSelection()
                    .isNotEmpty() || viewPagerAdapter.getTeachersSelection().isNotEmpty()
            ) {
                if (binding.tabLayout.selectedTabPosition == UserTypeEnum.STUDENT.position) {
                    viewModel.deleteUsersFromGroup(
                        args.groupUid,
                        viewPagerAdapter.getStudentSelection(),
                        UserTypeEnum.STUDENT
                    )
                } else {
                    viewModel.deleteUsersFromGroup(
                        args.groupUid,
                        viewPagerAdapter.getTeachersSelection(),
                        UserTypeEnum.TEACHER
                    )
                }
            } else {
                usersBottomSheet =
                    UsersBottomSheetFragment(UserTypeEnum.getTypeByPosition(binding.tabLayout.selectedTabPosition)) {
                        usersBottomSheet?.dismiss()
                        viewModel.addUserToGroup(args.groupUid, it.uid, it.userType)
                    }
                usersBottomSheet?.show(childFragmentManager, UsersBottomSheetFragment.TAG)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewPagerAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onUserClicked(user: User) {
        findNavController().navigate(
            GroupUsersFragmentDirections.actionFromGroupUsersToUserFragment(
                user.userType,
                user.uid,
                user.name,
                user.groupId.toTypedArray()
            )
        )
    }

    override fun getStudentsSelection(selectionList: List<String>) {
        if (selectionList.isNotEmpty()) {
            binding.floatingActionButton.setImageResource(R.drawable.ic_remove)
        } else {
            binding.floatingActionButton.setImageResource(R.drawable.icon_add)
        }
    }

    override fun getTeachersSelection(selectionList: List<String>) {
        if (selectionList.isNotEmpty()) {
            binding.floatingActionButton.setImageResource(R.drawable.ic_remove)
        } else {
            binding.floatingActionButton.setImageResource(R.drawable.icon_add)
        }
    }

}