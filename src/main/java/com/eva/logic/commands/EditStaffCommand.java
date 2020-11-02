package com.eva.logic.commands;

import static com.eva.commons.core.PanelState.STAFF_LIST;
import static com.eva.commons.core.PanelState.STAFF_PROFILE;
import static com.eva.logic.commands.EditCommand.MESSAGE_DUPLICATE_PERSON;
import static com.eva.logic.commands.EditCommand.MESSAGE_EDIT_PERSON_SUCCESS;
import static com.eva.logic.commands.EditCommand.createEditedPerson;
import static com.eva.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static com.eva.logic.parser.CliSyntax.PREFIX_COMMENT;
import static com.eva.logic.parser.CliSyntax.PREFIX_EMAIL;
import static com.eva.logic.parser.CliSyntax.PREFIX_NAME;
import static com.eva.logic.parser.CliSyntax.PREFIX_PHONE;
import static com.eva.logic.parser.CliSyntax.PREFIX_TAG;
import static com.eva.model.Model.PREDICATE_SHOW_ALL_STAFFS;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.eva.commons.core.Messages;
import com.eva.commons.core.PanelState;
import com.eva.commons.core.index.Index;
import com.eva.logic.commands.exceptions.CommandException;
import com.eva.model.Model;
import com.eva.model.current.view.CurrentViewStaff;
import com.eva.model.person.Person;
import com.eva.model.person.staff.Staff;

public class EditStaffCommand extends Command {
    public static final String COMMAND_WORD = "edits";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the details of the staff identified "
            + "by the index number used in the displayed staff list. "
            + "Existing values will be overwritten by the input values.\n"
            + "Parameters: INDEX (must be a positive integer) "
            + "[" + PREFIX_NAME + "NAME] "
            + "[" + PREFIX_PHONE + "PHONE] "
            + "[" + PREFIX_EMAIL + "EMAIL] "
            + "[" + PREFIX_ADDRESS + "ADDRESS] "
            + "[" + PREFIX_TAG + "TAG]"
            + "[" + PREFIX_COMMENT + "COMMENT]...\n"
            + "Example: " + COMMAND_WORD + " 1 "
            + PREFIX_PHONE + "91234567 "
            + PREFIX_EMAIL + "johndoe@example.com";

    public static final String MESSAGE_WRONG_PANEL = "Please switch to staff list panel "
            + "via 'list s-' to edit staff";

    private final Index index;
    private final EditCommand.EditPersonDescriptor editPersonDescriptor;

    /**
     * Creates edit staff command object
     * @param index
     * @param editPersonDescriptor
     */
    public EditStaffCommand(Index index, EditCommand.EditPersonDescriptor editPersonDescriptor) {
        requireNonNull(index);
        requireNonNull(editPersonDescriptor);

        this.index = index;
        this.editPersonDescriptor = new EditCommand.EditPersonDescriptor(editPersonDescriptor);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        PanelState panelState = model.getPanelState();
        if (!panelState.equals(STAFF_LIST) && !panelState.equals(STAFF_PROFILE)) {
            throw new CommandException(MESSAGE_WRONG_PANEL);
        }
        List<Staff> lastShownList = model.getFilteredStaffList();
        if (index.getZeroBased() >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }
        Staff personToEdit = lastShownList.get(index.getZeroBased());
        Person editedPerson = createEditedPerson(personToEdit, editPersonDescriptor);
        if (!personToEdit.isSamePerson(editedPerson) && model.hasPerson(editedPerson)) {
            throw new CommandException(MESSAGE_DUPLICATE_PERSON);
        }
        model.setStaff(personToEdit, (Staff) editedPerson);
        model.updateFilteredStaffList(PREDICATE_SHOW_ALL_STAFFS);
        if (panelState.equals(STAFF_PROFILE)) {
            Staff staffToView = lastShownList.get(index.getZeroBased());
            model.setCurrentViewStaff(new CurrentViewStaff(staffToView, index));
        }
        return new CommandResult(String.format(MESSAGE_EDIT_PERSON_SUCCESS, editedPerson),
                false, false, true);
    }
}
