package femr.ui.helpers.controller;

import com.google.inject.Inject;
import com.google.inject.Provider;
import femr.business.dtos.ServiceResponse;
import femr.business.services.ISearchService;
import femr.business.services.IUserService;
import femr.common.models.*;
import femr.ui.models.search.CreateEncounterViewModel;
import femr.util.DataStructure.VitalMultiMap;
import femr.util.DataStructure.Pair;
import femr.util.calculations.dateUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EncounterHelper {
    private Provider<IPatientEncounterTreatmentField> patientEncounterTreatmentFieldProvider;
    private Provider<IPatientEncounterHpiField> patientEncounterHpiFieldProvider;
    private Provider<IPatientEncounterPmhField> patientEncounterPmhFieldProvider;
    private Provider<IPatientPrescription> patientPrescriptionProvider;
    private Provider<IPatientEncounterVital> patientEncounterVitalProvider;
    private IUserService userService;
    private ISearchService searchService;

    @Inject
    public EncounterHelper(Provider<IPatientEncounterTreatmentField> patientEncounterTreatmentFieldProvider, Provider<IPatientEncounterHpiField> patientEncounterHpiFieldProvider, Provider<IPatientEncounterPmhField> patientEncounterPmhFieldProvider, Provider<IPatientPrescription> patientPrescriptionProvider, Provider<IPatientEncounterVital> patientEncounterVitalProvider,
                           IUserService userService, ISearchService searchService) {
        this.patientEncounterTreatmentFieldProvider = patientEncounterTreatmentFieldProvider;
        this.patientEncounterHpiFieldProvider = patientEncounterHpiFieldProvider;
        this.patientEncounterPmhFieldProvider = patientEncounterPmhFieldProvider;
        this.patientEncounterVitalProvider = patientEncounterVitalProvider;
        this.patientPrescriptionProvider = patientPrescriptionProvider;
        this.userService = userService; // Used to get the username associated with the values
        this.searchService = searchService; // used to retreive the replacement pharmacy name
    }

    public CreateEncounterViewModel populateViewModelGet(IPatient patient, IPatientEncounter patientEncounter, List<? extends IPatientPrescription> patientPrescriptions, VitalMultiMap patientEncounterVitalMap, Map<String, List<? extends IPatientEncounterTreatmentField>> patientEncounterTreatmentMap, Map<String, List<? extends IPatientEncounterHpiField>> patientEncounterHpiMap, Map<String, List<? extends IPatientEncounterPmhField>> patientEncounterPmhMap) {
        CreateEncounterViewModel viewModelGet = new CreateEncounterViewModel();
        //prescriptions

        // Create a list of pairs that have medication and the replacement medication if it exist for each entry
        List<Pair<String,String>> medicationAndReplacement = new LinkedList<>();
        List<Integer> ignoreList = new ArrayList<>(); // list used to make sure we ignore duplicate entries

        for (IPatientPrescription patientPrescription : patientPrescriptions) {
            // check if the medication was replaced if so save it and the replacement medications name
            if (patientPrescription.getReplaced()) {
                medicationAndReplacement.add(new Pair<String, String>(patientPrescription.getMedicationName(),
                        getPrescriptionNameById(patientPrescription.getReplacementId())));
                // add the replaced prescription id to the ignore list so we don't added it twice
                ignoreList.add(patientPrescription.getReplacementId());
            } else if (!ignoreList.contains(patientPrescription.getId())) {
                // if the medication is not in the ignore list
                medicationAndReplacement.add(new Pair<String, String>(patientPrescription.getMedicationName(), ""));
            }
        }


        // Set the doctor's first and last name
        viewModelGet.setDoctorFirstName(this.userService.findById(patientEncounter.getUserId()).getFirstName());
        viewModelGet.setDoctorLastName(this.userService.findById(patientEncounter.getUserId()).getLastName());
        // set the pharmacist first and last name
        viewModelGet.setPharmacistFirstName(null);
        viewModelGet.setPharmacistLastName(null);
        // if the size of the prescriptions list is greater or equal to 1 then we know a prescription was prescribed
        // and that their is a pharmacist name.  We also assume that the pharmacist who gave the first medication
        // gave all the medications
        if(patientPrescriptions.size() >= 1) {
            viewModelGet.setPharmacistFirstName(this.userService.findById(patientPrescriptions.get(0).getUserId()).getFirstName());
            viewModelGet.setPharmacistLastName(this.userService.findById(patientPrescriptions.get(0).getUserId()).getLastName());
        }
        //patient
        viewModelGet.setpID(patient.getId());
        viewModelGet.setCity(patient.getCity());
        viewModelGet.setFirstName(patient.getFirstName());
        viewModelGet.setLastName(patient.getLastName());
        viewModelGet.setAge(dateUtils.calculateYears(patient.getAge()));
        viewModelGet.setSex(patient.getSex());
        //patient encounter
        viewModelGet.setChiefComplaint(patientEncounter.getChiefComplaint());
        viewModelGet.setWeeksPregnant(patientEncounter.getWeeksPregnant());
        //editable information - prescriptions
        viewModelGet.setPrescription1(getOriginalPrescriptionOrNull(1, medicationAndReplacement));
        viewModelGet.setPrescription2(getOriginalPrescriptionOrNull(2, medicationAndReplacement));
        viewModelGet.setPrescription3(getOriginalPrescriptionOrNull(3, medicationAndReplacement));
        viewModelGet.setPrescription4(getOriginalPrescriptionOrNull(4, medicationAndReplacement));
        viewModelGet.setPrescription5(getOriginalPrescriptionOrNull(5, medicationAndReplacement));
        //viewModelGet.setMedications(viewMedications);

        // sets the Medication and Replacement Pair list
        viewModelGet.setMedicationAndReplacement(medicationAndReplacement);
        //editable information - Treatment_fields
        viewModelGet.setAssessment(getTreatmentFieldOrNull("assessment", patientEncounterTreatmentMap));
        viewModelGet.setProblem1(getTreatmentProblemOrNull(1, patientEncounterTreatmentMap));
        viewModelGet.setProblem2(getTreatmentProblemOrNull(2, patientEncounterTreatmentMap));
        viewModelGet.setProblem3(getTreatmentProblemOrNull(3, patientEncounterTreatmentMap));
        viewModelGet.setProblem4(getTreatmentProblemOrNull(4, patientEncounterTreatmentMap));
        viewModelGet.setProblem5(getTreatmentProblemOrNull(5, patientEncounterTreatmentMap));
        viewModelGet.setTreatment(getTreatmentFieldOrNull("treatment", patientEncounterTreatmentMap));
        viewModelGet.setFamilyHistory(getTreatmentFieldOrNull("familyHistory", patientEncounterTreatmentMap));
        //editable information - Hpi_fields
        viewModelGet.setOnset(getHpiFieldOrNull("onset", patientEncounterHpiMap));
        viewModelGet.setSeverity(getHpiFieldOrNull("severity", patientEncounterHpiMap));
        viewModelGet.setRadiation(getHpiFieldOrNull("radiation", patientEncounterHpiMap));
        viewModelGet.setQuality(getHpiFieldOrNull("quality", patientEncounterHpiMap));
        viewModelGet.setProvokes(getHpiFieldOrNull("provokes", patientEncounterHpiMap));
        viewModelGet.setPalliates(getHpiFieldOrNull("palliates", patientEncounterHpiMap));
        viewModelGet.setTimeOfDay(getHpiFieldOrNull("timeOfDay", patientEncounterHpiMap));
        viewModelGet.setPhysicalExamination(getHpiFieldOrNull("physicalExamination", patientEncounterHpiMap));
        viewModelGet.setNarrative(getHpiFieldOrNull("narrative", patientEncounterHpiMap));
        //editable information - Pmh_fields
        viewModelGet.setMedicalSurgicalHistory(getPmhFieldOrNull("medicalSurgicalHistory", patientEncounterPmhMap));
        viewModelGet.setSocialHistory(getPmhFieldOrNull("socialHistory", patientEncounterPmhMap));
        viewModelGet.setCurrentMedication(getPmhFieldOrNull("currentMedication", patientEncounterPmhMap));
        viewModelGet.setFamilyHistory(getPmhFieldOrNull("familyHistory", patientEncounterPmhMap));
        //vitals
        viewModelGet.setVitalList(patientEncounterVitalMap);
//        viewModelGet.setRespiratoryRate(getIntVitalOrNull("respiratoryRate", patientEncounterVitalMap));
//        viewModelGet.setHeartRate(getIntVitalOrNull("heartRate", patientEncounterVitalMap));
//        viewModelGet.setHeightFeet(getIntVitalOrNull("heightFeet", patientEncounterVitalMap));
//        viewModelGet.setHeightInches(getIntVitalOrNull("heightInches", patientEncounterVitalMap));
//        viewModelGet.setBloodPressureSystolic(getIntVitalOrNull("bloodPressureSystolic", patientEncounterVitalMap));
//        viewModelGet.setBloodPressureDiastolic(getIntVitalOrNull("bloodPressureDiastolic", patientEncounterVitalMap));
//        viewModelGet.setTemperature(getFloatVitalOrNull("temperature", patientEncounterVitalMap));
//        viewModelGet.setOxygenSaturation(getFloatVitalOrNull("oxygenSaturation", patientEncounterVitalMap));
//        viewModelGet.setWeight(getFloatVitalOrNull("weight", patientEncounterVitalMap));
//        viewModelGet.setGlucose(getFloatVitalOrNull("glucose", patientEncounterVitalMap));
        //Medication
        return viewModelGet;
    }

    //region **get encounter fields**
    public IPatientPrescription getPatientPrescription(int userId, int patientEncounterId, String name){
        IPatientPrescription patientPrescription = patientPrescriptionProvider.get();
        patientPrescription.setEncounterId(patientEncounterId);
        patientPrescription.setUserId(userId);
        patientPrescription.setReplaced(false);
        patientPrescription.setReplacementId(null);
        patientPrescription.setDateTaken(dateUtils.getCurrentDateTime());
        patientPrescription.setMedicationName(name);
        return patientPrescription;
    }
    public IPatientEncounterVital getPatientEncounterVital(int userId, int patientEncounterId, IVital vital, double vitalValue) {
        IPatientEncounterVital patientEncounterVital = patientEncounterVitalProvider.get();
        patientEncounterVital.setUserId(userId);
        patientEncounterVital.setPatientEncounterId(patientEncounterId);
        patientEncounterVital.setVital(vital);
        patientEncounterVital.setVitalValue((float) vitalValue);
        patientEncounterVital.setDateTaken(dateUtils.getCurrentDateTimeString());
        return patientEncounterVital;
    }

    public IPatientEncounterTreatmentField getPatientEncounterTreatmentField(int userId, int patientEncounterId, ITreatmentField treatmentField, String treatmentValue) {
        IPatientEncounterTreatmentField patientEncounterTreatmentField = patientEncounterTreatmentFieldProvider.get();
        patientEncounterTreatmentField.setUserId(userId);
        patientEncounterTreatmentField.setPatientEncounterId(patientEncounterId);
        patientEncounterTreatmentField.setTreatmentField(treatmentField);
        patientEncounterTreatmentField.setTreatmentFieldValue(treatmentValue.trim());
        patientEncounterTreatmentField.setDateTaken(dateUtils.getCurrentDateTime());
        return patientEncounterTreatmentField;
    }

    public IPatientEncounterPmhField getPatientEncounterPmhField(int userId, int patientEncounterId, IPmhField pmhField, String pmhValue) {
        IPatientEncounterPmhField patientEncounterPmhField = patientEncounterPmhFieldProvider.get();
        patientEncounterPmhField.setUserId(userId);
        patientEncounterPmhField.setPatientEncounterId(patientEncounterId);
        patientEncounterPmhField.setPmhField(pmhField);
        patientEncounterPmhField.setPmhFieldValue(pmhValue.trim());
        patientEncounterPmhField.setDateTaken(dateUtils.getCurrentDateTime());
        return patientEncounterPmhField;
    }

    public IPatientEncounterHpiField getPatientEncounterHpiField(int userId, int patientEncounterId, IHpiField hpiField, String hpiValue) {
        IPatientEncounterHpiField patientEncounterHpiField = patientEncounterHpiFieldProvider.get();
        patientEncounterHpiField.setUserId(userId);
        patientEncounterHpiField.setPatientEncounterId(patientEncounterId);
        patientEncounterHpiField.setHpiField(hpiField);
        patientEncounterHpiField.setHpiFieldValue(hpiValue);
        patientEncounterHpiField.setDateTaken(dateUtils.getCurrentDateTime());
        return patientEncounterHpiField;
    }
    //endregion

    private String getDoctorFirstNameOrNull(IHpiField hpiField, IPmhField pmhField, ITreatmentField treatmentField) {
        // check to see if the fields are filled out if so find the name of the doctor who filled it out
        IPatientEncounterHpiField patientEncounterHpiField = patientEncounterHpiFieldProvider.get();
        if(patientEncounterHpiField.getUserId() != null)
        {
            
        }
    }

    //region **get value or get null**
    private String getOriginalPrescriptionOrNull(int number, List<Pair<String, String>> patientPrescriptions) {
        if (patientPrescriptions.size() >= number) {
            return patientPrescriptions.get(number - 1).getKey();
        } else {
            return null;
        }
    }

    /**
     * given the id for a prescription the function will return its name.
     * used for getting the name of the replacement prescription
     * @param id the id of the prescription
     * @return The name of the Prescription as a string
     */
    private String getPrescriptionNameById(int id) {
        ServiceResponse<IPatientPrescription> patientPrescriptionServiceResponse = searchService.findPatientPrescriptionById(id);
        IPatientPrescription patientPrescription = patientPrescriptionServiceResponse.getResponseObject();
        return patientPrescription.getMedicationName();
    }

    // get the replaced boolean for a perscription or return false by default
    private Boolean getReplacedOrNull(int number, List<? extends IPatientPrescription> patientPrescriptions){
        if(patientPrescriptions.size() >= number) {
            return patientPrescriptions.get(number - 1).getReplaced();
        }
        else{
            return false;
        }
    }



    // The map has 2 keys the first key holds the name of the vital the second key holds the Date it was entered
//    private Integer getIntVitalOrNull(String key,MultiKeyMap<String, String, IPatientEncounterVital> patientEncounterVitalMap) {
//        if (patientEncounterVitalMap.containsKey1(key)) {
//            if (patientEncounterVitalMap.get(key).size() < 1) {
//                return null;
//            }
//            return patientEncounterVitalMap.get(key).get(0).getVitalValue().intValue();
//        }
//        return null;
//    }
//
//    private Float getFloatVitalOrNull(String key, Map<String, List<? extends IPatientEncounterVital>> patientEncounterVitalMap) {
//        if (patientEncounterVitalMap.containsKey(key)) {
//            if (patientEncounterVitalMap.get(key).size() < 1) {
//                return null;
//            }
//            return patientEncounterVitalMap.get(key).get(0).getVitalValue();
//        }
//        return null;
//    }

    private String getHpiFieldOrNull(String key, Map<String, List<? extends IPatientEncounterHpiField>> patientEncounterHpiMap) {
        if (patientEncounterHpiMap.containsKey(key)) {
            if (patientEncounterHpiMap.get(key).size() < 1) {
                return null;
            }
            return patientEncounterHpiMap.get(key).get(0).getHpiFieldValue().trim();
        }
        return null;
    }

    private String getPmhFieldOrNull(String key, Map<String, List<? extends IPatientEncounterPmhField>> patientEncounterPmhMap) {
        if (patientEncounterPmhMap.containsKey(key)) {
            if (patientEncounterPmhMap.get(key).size() < 1) {
                return null;
            }
            return patientEncounterPmhMap.get(key).get(0).getPmhFieldValue().trim();
        }
        return null;
    }


    private String getTreatmentFieldOrNull(String key, Map<String, List<? extends IPatientEncounterTreatmentField>> patientEncounterTreatmentMap) {
        if (patientEncounterTreatmentMap.containsKey(key)) {
            if (patientEncounterTreatmentMap.get(key).size() < 1) {
                return null;
            }
            return patientEncounterTreatmentMap.get(key).get(0).getTreatmentFieldValue().trim();
        }
        return null;
    }

    private String getTreatmentProblemOrNull(int problem, Map<String, List<? extends IPatientEncounterTreatmentField>> patientEncounterTreatmentMap) {
        if (patientEncounterTreatmentMap.containsKey("problem")) {
            if (patientEncounterTreatmentMap.get("problem").size() >= problem) {
                return patientEncounterTreatmentMap.get("problem").get(problem - 1).getTreatmentFieldValue();
            }
            return null;
        }
        return null;
    }
    //endregion
}
