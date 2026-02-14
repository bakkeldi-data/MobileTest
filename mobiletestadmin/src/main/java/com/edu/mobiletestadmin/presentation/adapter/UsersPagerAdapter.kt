package com.edu.mobiletestadmin.presentation.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.edu.mobiletestadmin.R
import com.edu.mobiletestadmin.databinding.ItemViewPagerUserBinding
import com.edu.mobiletestadmin.presentation.model.UserTypeEnum
import com.edu.mobiletestadmin.utils.addDividerItemDecoration

class UsersPagerAdapter(
    private val studentsAdapter: UsersAdapter,
    private val teachersAdapter: UsersAdapter,
    private val selectionListener: SelectionListener? = null,
    private val retryListener: RetryListener? = null,
    private val bundle: Bundle? = null,
    private val noTeachersMessage: Int,
    private val noStudentsMessage: Int

) : RecyclerView.Adapter<UsersPagerAdapter.UserPageVH>() {

    companion object {
        const val PAGES_COUNT = 2
        const val STUDENTS = 0
        const val TEACHERS = 1
    }


    private var studentsTracker: SelectionTracker<String>? = null
    private var teachersTracker: SelectionTracker<String>? = null

    inner class UserPageVH(private val binding: ItemViewPagerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            position: Int
        ) {

            binding.viewPagerRv.adapter = when (position) {
                STUDENTS -> studentsAdapter
                TEACHERS -> teachersAdapter
                else -> null
            }
            binding.viewPagerRv.addDividerItemDecoration()

            when (position) {
                STUDENTS -> {
                    studentsTracker = buildSelectionTracker(binding, position, studentsAdapter)
                    studentsAdapter.tracker = studentsTracker
                }
                TEACHERS -> {
                    teachersTracker = buildSelectionTracker(binding, position, teachersAdapter)
                    teachersAdapter.tracker = teachersTracker
                }
            }

            studentsTracker?.onRestoreInstanceState(bundle)
            teachersTracker?.onRestoreInstanceState(bundle)

            studentsTracker?.addObserver(object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    studentsTracker?.let {
                        selectionListener?.getStudentsSelection(it.selection.toList())
                    }
                }
            })

            teachersTracker?.addObserver(object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    teachersTracker?.let {
                        selectionListener?.getTeachersSelection(it.selection.toList())
                    }
                }
            })

            val usersType = if (position == STUDENTS) UserTypeEnum.STUDENT else UserTypeEnum.TEACHER
            binding.retryButton.isVisible = retryListener != null
            binding.retryButton.setOnClickListener {
                retryListener?.onRetryClick(usersType)
            }
            render(tabState[position] ?: PageState.Content, usersType)
        }

        fun render(state: PageState, usersType: UserTypeEnum) {
            when (state) {
                is PageState.Content -> {
                    binding.loader.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.message.isVisible = false
                    binding.viewPagerRv.isVisible = true
                }
                is PageState.Loading -> {
                    binding.loader.isVisible = true
                    binding.errorContainer.isVisible = false
                    binding.message.isVisible = false
                    binding.viewPagerRv.isVisible = true
                }
                is PageState.Empty -> {
                    binding.loader.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.viewPagerRv.isVisible = false
                    binding.message.isVisible = true
                    binding.message.text = if (usersType == UserTypeEnum.STUDENT) {
                        binding.root.resources.getString(noStudentsMessage)
                    } else {
                        binding.root.resources.getString(noTeachersMessage)
                    }
                }
                is PageState.Error -> {
                    binding.loader.isVisible = false
                    binding.message.isVisible = false
                    binding.viewPagerRv.isVisible = false
                    binding.errorContainer.isVisible = true
                    binding.errorMessage.text = state.message?.takeIf { it.isNotBlank() }
                        ?: binding.root.resources.getString(R.string.error_generic)
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPageVH {

        val viewHolder = UserPageVH(
            ItemViewPagerUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        holderMap[viewType] = viewHolder
        return viewHolder
    }

    override fun onBindViewHolder(holder: UserPageVH, position: Int) {
        holder.bind(position)
    }


    private val holderMap: HashMap<Int, UserPageVH> = hashMapOf()

    private val tabState: HashMap<Int, PageState> = hashMapOf(STUDENTS to PageState.Content, TEACHERS to PageState.Content)

    fun renderState(state: PageState, usersType: UserTypeEnum) {
        val position = UserTypeEnum.getPositionByType(usersType)
        tabState[position] = state
        holderMap[position]?.render(state, usersType)
    }

    fun getStudentSelection(): List<String> {
        return studentsTracker?.selection?.toList() ?: emptyList()
    }

    fun getTeachersSelection(): List<String> {
        return teachersTracker?.selection?.toList() ?: emptyList()
    }

    fun buildSelectionTracker(
        binding: ItemViewPagerUserBinding,
        position: Int,
        adapter: UsersAdapter
    ) = SelectionTracker.Builder(
        //1
        "selectionItem$position",
        //2
        binding.viewPagerRv,
        //3
        ItemsKeyProvider(
            adapter
        ),
        ItemsDetailsLookup(binding.viewPagerRv),
        //4
        StorageStrategy.createStringStorage()
    ).withSelectionPredicate(
        //5
        SelectionPredicates.createSelectAnything()
    ).build()

    fun saveInstanceState(outState: Bundle) {
        studentsTracker?.onSaveInstanceState(outState)
        teachersTracker?.onSaveInstanceState(outState)
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> STUDENTS
            else -> TEACHERS
        }
    }

    override fun getItemCount() = PAGES_COUNT

    sealed class PageState {
        object Loading : PageState()
        object Content : PageState()
        object Empty : PageState()
        data class Error(val message: String?) : PageState()
    }

    interface SelectionListener {
        fun getStudentsSelection(selectionList: List<String>)
        fun getTeachersSelection(selectionList: List<String>)
    }

    interface RetryListener {
        fun onRetryClick(usersType: UserTypeEnum)
    }
}