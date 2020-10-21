package com.eva;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import com.eva.commons.core.Config;
import com.eva.commons.core.LogsCenter;
import com.eva.commons.core.Version;
import com.eva.commons.exceptions.DataConversionException;
import com.eva.commons.util.ConfigUtil;
import com.eva.commons.util.StringUtil;
import com.eva.logic.Logic;
import com.eva.logic.LogicManager;
import com.eva.model.EvaDatabase;
import com.eva.model.Model;
import com.eva.model.ModelManager;
import com.eva.model.ReadOnlyEvaDatabase;
import com.eva.model.ReadOnlyUserPrefs;
import com.eva.model.UserPrefs;
import com.eva.model.person.Person;
import com.eva.model.person.applicant.Applicant;
import com.eva.model.person.staff.Staff;
import com.eva.model.util.SampleDataUtil;
import com.eva.storage.EvaStorage;
import com.eva.storage.JsonEvaStorage;
import com.eva.storage.JsonUserPrefsStorage;
import com.eva.storage.Storage;
import com.eva.storage.StorageManager;
import com.eva.storage.UserPrefsStorage;
import com.eva.ui.Ui;
import com.eva.ui.UiManager;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final Version VERSION = new Version(0, 6, 0, true);

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;

    @Override
    public void init() throws Exception {
        assert false;
        logger.info("=============================[ Initializing EvaDatabase ]===========================");
        super.init();

        AppParameters appParameters = AppParameters.parse(getParameters());
        config = initConfig(appParameters.getConfigPath());

        UserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(config.getUserPrefsFilePath());
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        EvaStorage evaStorage = new JsonEvaStorage(userPrefs.getPersonDatabaseFilePath(),
                userPrefs.getStaffDatabaseFilePath(), userPrefs.getApplicantDatabaseFilePath());
        storage = new StorageManager(evaStorage, userPrefsStorage);

        initLogging(config);

        model = initModelManager(storage, userPrefs);

        logic = new LogicManager(model, storage);

        ui = new UiManager(logic);
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s eva database and {@code userPrefs}. <br>
     * The data from the sample eva database will be used instead if {@code storage}'s eva database is not found,
     * or an empty eva database will be used instead if errors occur when reading {@code storage}'s eva database.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        Optional<ReadOnlyEvaDatabase<Person>> personDatabaseOptional;
        ReadOnlyEvaDatabase<Person> initialPersonData;
        Optional<ReadOnlyEvaDatabase<Staff>> staffDatabaseOptional;
        ReadOnlyEvaDatabase<Staff> initialStaffData;
        Optional<ReadOnlyEvaDatabase<Applicant>> applicantDatabaseOptional;
        ReadOnlyEvaDatabase<Applicant> initialApplicantData;
        try {
            personDatabaseOptional = storage.readPersonDatabase();
            staffDatabaseOptional = storage.readStaffDatabase();
            applicantDatabaseOptional = storage.readApplicantDatabase();
            if (personDatabaseOptional.isEmpty() || staffDatabaseOptional.isEmpty()
                    || applicantDatabaseOptional.isEmpty()) {
                logger.info("Data file not found. Will be starting with a sample EvaDatabase");
            }
            initialPersonData = personDatabaseOptional.orElseGet(SampleDataUtil::getSamplePersonDatabase);
            initialStaffData = staffDatabaseOptional.orElseGet(SampleDataUtil::getSampleStaffDatabase);
            initialApplicantData = applicantDatabaseOptional.orElseGet(SampleDataUtil::getSampleApplicantDatabase);
        } catch (DataConversionException e) {
            logger.warning("Data file not in the correct format. Will be starting with an empty EvaDatabase");
            initialPersonData = new EvaDatabase<>();
            initialStaffData = new EvaDatabase<>();
            initialApplicantData = new EvaDatabase<>();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty EvaDatabase");
            initialPersonData = new EvaDatabase<>();
            initialStaffData = new EvaDatabase<>();
            initialApplicantData = new EvaDatabase<>();
        }

        return new ModelManager(initialPersonData, initialStaffData, initialApplicantData, userPrefs);
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Returns a {@code Config} using the file at {@code configFilePath}. <br>
     * The default file path {@code Config#DEFAULT_CONFIG_FILE} will be used instead
     * if {@code configFilePath} is null.
     */
    protected Config initConfig(Path configFilePath) {
        Config initializedConfig;
        Path configFilePathUsed;

        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFilePathUsed + " is not in the correct format. "
                    + "Using default config properties");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs file path,
     * or a new {@code UserPrefs} with default configuration if errors occur when
     * reading from the file.
     */
    protected UserPrefs initPrefs(UserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct format. "
                    + "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty EvaDatabase");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting EvaDatabase " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping eva database ] =============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
