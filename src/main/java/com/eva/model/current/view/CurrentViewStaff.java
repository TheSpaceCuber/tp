package com.eva.model.current.view;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import com.eva.model.comment.Comment;
import com.eva.model.comment.CommentNotFoundException;
import com.eva.model.person.UniqueCommentsList;
import com.eva.model.person.staff.Staff;
import com.eva.model.person.staff.leave.Leave;
import com.eva.model.person.staff.leave.UniqueLeaveList;
import com.eva.model.person.staff.leave.exceptions.LeaveNotFoundException;

import javafx.collections.ObservableList;

public class CurrentViewStaff {

    private final Staff currentView;
    private final UniqueLeaveList leaves;
    private final UniqueCommentsList comments;

    /**
     * Creates an empty currentViewStaff.
     */
    public CurrentViewStaff() {
        this.currentView = null;
        this.leaves = new UniqueLeaveList();
        this.comments = new UniqueCommentsList();
    }

    /**
     * Creates a currentView with the current viewing staff.
     */
    public CurrentViewStaff(Staff currentView) {
        requireNonNull(currentView);
        this.currentView = currentView;
        this.leaves = new UniqueLeaveList();
        this.leaves.fill(currentView.getLeaves());
        this.comments = new UniqueCommentsList();
        this.comments.fill(currentView.getComments());
    }

    public Optional<Staff> getCurrentView() {
        return Optional.ofNullable(currentView);
    }

    public ObservableList<Comment> getCommentList() throws CommentNotFoundException {
        if (currentView == null) {
            throw new CommentNotFoundException();
        }
        return comments.asUnmodifiableObservableList();
    }

    public ObservableList<Leave> getLeaveList() throws LeaveNotFoundException {
        if (currentView == null) {
            throw new LeaveNotFoundException();
        }
        return leaves.asUnmodifiableObservableList();
    }

    @Override
    public String toString() {
        return leaves.asUnmodifiableObservableList().size() + " leaves";
        // TODO: refine later
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof CurrentViewStaff // instanceof handles nulls
                && leaves.equals(((CurrentViewStaff) other).leaves)
                && comments.equals(((CurrentViewStaff) other).comments)
                && currentView.equals(((CurrentViewStaff) other).currentView));
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentView, leaves);
    }
}
