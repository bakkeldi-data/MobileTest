package com.edu.mobiletestadmin.presentation.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edu.mobiletestadmin.R
import com.edu.mobiletestadmin.databinding.FragmentUsersBinding
import com.edu.mobiletestadmin.presentation.adapter.UsersAdapter
import com.edu.mobiletestadmin.presentation.adapter.UsersPagerAdapter
import com.edu.mobiletestadmin.presentation.model.ResourceState
import com.edu.mobiletestadmin.presentation.model.User
import com.edu.mobiletestadmin.presentation.model.UserTypeEnum
import com.edu.mobiletestadmin.presentation.viewModel.UsersViewModel
import com.edu.mobiletestadmin.utils.*
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class UsersFragment : Fragment(R.layout.fragment_users), UsersAdapter.Listener,
    UsersPagerAdapter.SelectionListener, UsersPagerAdapter.RetryListener {

    private val binding by viewBinding(FragmentUsersBinding::bind)

    private val viewModel by viewModel<UsersViewModel>()

    private val imageLoader: IImageLoader by inject()

    private val studentsAdapter by lazy {
        UsersAdapter(this, imageLoader)
    }

    private val teachersAdapter by lazy {
        UsersAdapter(this, imageLoader)
    }

    private var viewPagerAdapter: UsersPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getStudents()
        viewModel.getTeachers()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPagerAdapter =
            UsersPagerAdapter(
                studentsAdapter,
                teachersAdapter,
                this,
                this,
                savedInstanceState,
                noTeachersMessage = R.string.no_teachers,
                noStudentsMessage = R.string.no_students
            )

        binding.viewPager.adapter = viewPagerAdapter

        binding.floatingActionButton.setOnClickListener {

            if (binding.tabLayout.selectedTabPosition == UserTypeEnum.STUDENT.position) {
                val studentsList = viewPagerAdapter?.getStudentSelection() ?: emptyList()
                if (studentsList.isNotEmpty()) {
                    requireContext().buildMaterialAlertDialog(
                        if (studentsList.size == 1) resources.getString(R.string.delete_student) else resources.getString(
                            R.string.delete_students
                        ), positiveBtnClick = {
                            viewModel.deleteStudents(studentsList)
                        }
                    ).show()
                } else {
                    findNavController().navigate(
                        UsersFragmentDirections.actionFromUsersFragmentToUserFragment(
                            UserTypeEnum.STUDENT
                        )
                    )
                }

            } else {
                val teachersList = viewPagerAdapter?.getTeachersSelection() ?: emptyList()
                if (teachersList.isNotEmpty()) {
                    requireContext().buildMaterialAlertDialog(
                        if (teachersList.size == 1) resources.getString(R.string.delete_teacher) else resources.getString(
                            R.string.delete_teachers
                        ),
                        positiveBtnClick = {
                            viewModel.deleteTeachers(teachersList)
                        }
                    ).show()
                } else {
                    findNavController().navigate(
                        UsersFragmentDirections.actionFromUsersFragmentToUserFragment(
                            UserTypeEnum.TEACHER
                        )
                    )
                }
            }
        }

        binding.searchView.addQueryChangeListener { query ->
            viewModel.searchUsers(
                query,
                UserTypeEnum.getTypeByPosition(binding.tabLayout.selectedTabPosition)
            )
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (UserTypeEnum.getTypeByPosition(position)) {
                UserTypeEnum.STUDENT -> resources.getString(R.string.students)
                UserTypeEnum.TEACHER -> resources.getString(R.string.teachers)
            }
        }.attach()

        viewModel.studentsLiveData.observe(viewLifecycleOwner) { state ->

            when (state) {
                is ResourceState.Success -> {
                    if (state.data.isEmpty()) {
                        viewPagerAdapter?.renderState(
                            UsersPagerAdapter.PageState.Empty,
                            UserTypeEnum.STUDENT
                        )
                        studentsAdapter.submitList(emptyList())
                    } else {
                        viewPagerAdapter?.renderState(
                            UsersPagerAdapter.PageState.Content,
                            UserTypeEnum.STUDENT
                        )
                        studentsAdapter.submitList(state.data)
                    }
                }
                is ResourceState.Empty -> {
                    viewPagerAdapter?.renderState(UsersPagerAdapter.PageState.Empty, UserTypeEnum.STUDENT)
                    studentsAdapter.submitList(emptyList())
                }
                is ResourceState.Error -> {
                    viewPagerAdapter?.renderState(
                        UsersPagerAdapter.PageState.Error(state.error),
                        UserTypeEnum.STUDENT
                    )
                }
                is ResourceState.Loading -> {
                    viewPagerAdapter?.renderState(UsersPagerAdapter.PageState.Loading, UserTypeEnum.STUDENT)
                }
                else -> Unit
            }
        }

        viewModel.teachersLiveData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Success -> {
                    if (state.data.isEmpty()) {
                        viewPagerAdapter?.renderState(
                            UsersPagerAdapter.PageState.Empty,
                            UserTypeEnum.TEACHER
                        )
                        teachersAdapter.submitList(emptyList())
                    } else {
                        viewPagerAdapter?.renderState(
                            UsersPagerAdapter.PageState.Content,
                            UserTypeEnum.TEACHER
                        )
                        teachersAdapter.submitList(state.data)
                    }
                }
                is ResourceState.Empty -> {
                    viewPagerAdapter?.renderState(UsersPagerAdapter.PageState.Empty, UserTypeEnum.TEACHER)
                    teachersAdapter.submitList(emptyList())
                }
                is ResourceState.Error -> {
                    viewPagerAdapter?.renderState(
                        UsersPagerAdapter.PageState.Error(state.error),
                        UserTypeEnum.TEACHER
                    )
                }
                is ResourceState.Loading -> {
                    viewPagerAdapter?.renderState(UsersPagerAdapter.PageState.Loading, UserTypeEnum.TEACHER)
                }
                else -> Unit
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) {
            binding.loader.root.isVisible = it
            binding.loader.progressBarCenter.isVisible = it
        }

        viewModel.error.observe(viewLifecycleOwner) {
            showToast(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewPagerAdapter?.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onUserClicked(user: User) {
        findNavController().navigate(
            UsersFragmentDirections.actionFromUsersFragmentToUserFragment(
                user.userType,
                user.uid,
                user.name,
                user.groupId.toTypedArray()
            )
        )
    }

    override fun getStudentsSelection(selectionList: List<String>) {
        showHideRemoveButton(selectionList.size)
    }

    override fun getTeachersSelection(selectionList: List<String>) {
        showHideRemoveButton(selectionList.size)
    }

    override fun onRetryClick(usersType: UserTypeEnum) {
        when (usersType) {
            UserTypeEnum.STUDENT -> viewModel.getStudents()
            UserTypeEnum.TEACHER -> viewModel.getTeachers()
        }
    }

    private fun showHideRemoveButton(selectionSize: Int) {
        if (selectionSize > 0) {
            binding.floatingActionButton.setImageResource(R.drawable.ic_remove)
        } else {
            binding.floatingActionButton.setImageResource(R.drawable.icon_add)
        }
    }
}