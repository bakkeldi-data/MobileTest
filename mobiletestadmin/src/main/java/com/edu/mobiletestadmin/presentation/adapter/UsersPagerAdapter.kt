package com.edu.mobiletestadmin.presentation.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.edu.mobiletestadmin.databinding.ItemViewPagerUserBinding
import com.edu.mobiletestadmin.presentation.model.UserTypeEnum
import com.edu.mobiletestadmin.utils.addDividerItemDecoration

class UsersPagerAdapter(
    private val studentsAdapter: UsersAdapter,
    private val teachersAdapter: UsersAdapter,
    private val selectionListener: SelectionListener? = null,
    private val bundle: Bundle? = null,
    private val noTeachersMessage: Int,
    private val noStudentsMessage: Int,
    private val onRetry: ((UserTypeEnum) -> Unit)? = null

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


            val usersType =
                if (position == STUDENTS) UserTypeEnum.STUDENT else UserTypeEnum.TEACHER
            val currentState = tabPageState[position] ?: PageDataState.NOT_EMPTY
            reactToDataSetSize(currentState, usersType, tabErrorMessages[position])
        }

        fun reactToDataSetSize(
            pageDataState: PageDataState,
            usersType: UserTypeEnum,
            errorMessage: String? = null
        ) {
            when (pageDataState) {
                PageDataState.LOADING -> {
                    binding.viewPagerRv.isVisible = false
                    binding.message.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.loader.isVisible = true
                }
                PageDataState.EMPTY -> {
                    binding.loader.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.viewPagerRv.isVisible = false
                    binding.message.isVisible = true
                    if (usersType == UserTypeEnum.STUDENT) {
                        binding.message.text =
                            binding.root.resources.getString(noStudentsMessage)
                    } else {
                        binding.message.text =
                            binding.root.resources.getString(noTeachersMessage)
                    }
                }
                PageDataState.NOT_EMPTY -> {
                    binding.loader.isVisible = false
                    binding.message.isVisible = false
                    binding.errorContainer.isVisible = false
                    binding.viewPagerRv.isVisible = true
                }
                PageDataState.ERROR -> {
                    binding.loader.isVisible = false
                    binding.message.isVisible = false
                    binding.viewPagerRv.isVisible = false
                    binding.errorContainer.isVisible = true
                    binding.errorMessage.text = errorMessage
                        ?: binding.root.resources.getString(
                            com.edu.mobiletestadmin.R.string.error_loading_data
                        )
                    binding.retryButton.setOnClickListener {
                        onRetry?.invoke(usersType)
                    }
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


    private val tabPageState = hashMapOf(
        STUDENTS to PageDataState.NOT_EMPTY,
        TEACHERS to PageDataState.NOT_EMPTY
    )
    private val tabErrorMessages = hashMapOf<Int, String?>()

    fun updatePageState(
        dataState: PageDataState,
        usersType: UserTypeEnum,
        errorMessage: String? = null
    ) {
        val position = UserTypeEnum.getPositionByType(usersType)
        tabPageState[position] = dataState
        tabErrorMessages[position] = errorMessage
        holderMap[position]?.reactToDataSetSize(dataState, usersType, errorMessage)
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

    enum class PageDataState {
        LOADING,
        EMPTY,
        NOT_EMPTY,
        ERROR
    }

    interface SelectionListener {
        fun getStudentsSelection(selectionList: List<String>)
        fun getTeachersSelection(selectionList: List<String>)
    }
}