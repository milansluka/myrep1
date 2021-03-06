package ehc.bo.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import ehc.bo.Resource;
import ehc.bo.SchedulingPolicy;
import ehc.util.DateUtil;

public class AppointmentScheduler {
	public static final String TREATMENTS_CANNOT_BE_JOINED_MSG = "Treatments can not be planned in one appointment, it is recommended to create more appointments";
	public static final String INSUFFICIENT_DIFFERENCE_BETWEEN_FROM_AND_TO_MSG = "Treatment type duration is longer than set duration";
	public static final String TREATMENTS_LIST_EMPTY_MSG = "No treatment types specified ";
	private PhysicianDao physicianDao = PhysicianDao.getInstance();
	private NurseDao nurseDao = NurseDao.getInstance();
	private DeviceDao deviceDao = DeviceDao.getInstance();
	private WorkTime workTime;
	private int timeGridInMinutes;
	private String message;
	private SchedulingPolicy policy;
	private SchedulingUtil schedulingUtil = new SchedulingUtil();

	public AppointmentScheduler(WorkTime workTime, int timeGridInMinutes) {
		this(workTime, timeGridInMinutes, new BasicPolicy());
	}

	public AppointmentScheduler(WorkTime workTime, int timeGridInMinutes, SchedulingPolicy policy) {
		super();
		this.workTime = workTime;
		this.timeGridInMinutes = timeGridInMinutes;
		this.policy = policy;
	}

	public Appointment getAppointment(User executor, AppointmentScheduleData appointmentScheduleData,
			List<TreatmentType> treatmentTypes, Individual individual) {
		Appointment appointment = new Appointment(executor, appointmentScheduleData.getFrom(),
				appointmentScheduleData.getTo(), individual);
		appointment.setResources(appointmentScheduleData.getResources());
		appointment.setPlannedTreatmentTypes(treatmentTypes);
		return appointment;
	}


	private boolean sufficientAppointmentDuration(Date from, Date to, List<TreatmentType> treatmentTypes) {
		long appointmentDuration = schedulingUtil.getAppointmentDurationInSeconds(from, to);
		return sufficientAppointmentDuration(appointmentDuration, treatmentTypes);
	}

	private boolean sufficientAppointmentDuration(long plannedAppointmentDuration, List<TreatmentType> treatmentTypes) {
		long treatmentDuration = 0;
		for (TreatmentType treatmentType : treatmentTypes) {
			treatmentDuration += treatmentType.getDuration();
		}
		return plannedAppointmentDuration >= treatmentDuration;
	}

//	private long getAppointmentDuration(Date from, Date to) {
//		return (to.getTime() - from.getTime()) / 1000;
//	}

	private boolean areResourcesAvailable(Date from, Date to, List<Resource> resources) {
		for (Resource resource : resources) {
			if (!resource.isNotBusy(from, to)) {
				return false;
			}
		}
		return true;
	}
	
	public AppointmentScheduleData getAppointmentScheduleData(Date from, Date to, List<TreatmentType> treatmentTypes,
			List<Resource> resources) {
		from = schedulingUtil.moveFromIfOutOfWorkTime(from, to, workTime);
		to = DateUtil.addSeconds(from, (int) schedulingUtil.getAppointmentDurationInSeconds(from, to));
		if (areResourcesAvailable(from, to, resources)) {
			return new AppointmentScheduleData(from, to, resources);
		}
		return null;
	}

	public List<AppointmentScheduleData> getAppointmentScheduleData(Date from, Date to,
			List<TreatmentType> treatmentTypes, List<Resource> resources, int count) {
		from = schedulingUtil.moveFromIfOutOfWorkTime(from, to, workTime);
		to = DateUtil.addSeconds(from, (int) schedulingUtil.getAppointmentDurationInSeconds(from, to));
		List<AppointmentScheduleData> appointmentScheduleDatas = new ArrayList<AppointmentScheduleData>();
		int givenAppointmentScheduleData = 0;
		while (givenAppointmentScheduleData < count) {
			if (areResourcesAvailable(from, to, resources)) {
				appointmentScheduleDatas.add(new AppointmentScheduleData(from, to, resources));
				givenAppointmentScheduleData++;
			}
			from = DateUtil.addSeconds(from, timeGridInMinutes * 60);
			to = DateUtil.addSeconds(to, timeGridInMinutes * 60);
		}
		return appointmentScheduleDatas;
	}

	public List<TimeWindow> findAvailableTimeWindows(Date from, Date to, List<TreatmentType> treatmentTypes,
			int appointmentDuration) {
		List<TimeWindow> foundBlocks = new ArrayList<TimeWindow>();

		if (treatmentTypes.isEmpty()) {
			setMessage(TREATMENTS_LIST_EMPTY_MSG);
			return foundBlocks;
		}

		if (!canBeCombinedIntoOneAppointment(treatmentTypes)) {
			setMessage(TREATMENTS_CANNOT_BE_JOINED_MSG);
			return foundBlocks;
		}

		if (!sufficientAppointmentDuration(from, to, treatmentTypes)) {
			setMessage(INSUFFICIENT_DIFFERENCE_BETWEEN_FROM_AND_TO_MSG);
			return foundBlocks;
		}

		/* long appointmentDuration = getAppointmentDuration(from, to); */
		from = schedulingUtil.moveFromIfOutOfWorkTime(from, to, workTime);
		/* to = DateUtil.addSeconds(from, (int) appointmentDuration); */

		List<Room> roomsWhereTreatmentTypesCanBeExecuted = findPossibleRoomsForTreatmentTypes(treatmentTypes);

		for (Room room : roomsWhereTreatmentTypesCanBeExecuted) {
			List<TimeWindow> blocks = findFreeTimeWindowsInRoom(from, to, treatmentTypes, room, appointmentDuration);
			foundBlocks.addAll(blocks);
		}

		return foundBlocks;
	}

	public List<TimeWindow> findFreeTimeWindowsInRoom(Date from, Date to, List<TreatmentType> treatmentTypes, Room room,
			long appointmentDuration) {
		List<TimeWindow> foundFreeTimeWindows = new ArrayList<TimeWindow>();
		Date actualPosition = (Date) from.clone();

		while (actualPosition.before(to)) {
			TimeWindow timeWindow = getNextTimeWindow(actualPosition, to, treatmentTypes, room, appointmentDuration);

			if (timeWindow != null) {
				foundFreeTimeWindows.add(timeWindow);
				actualPosition = timeWindow.getEnd();
			} else {
				actualPosition = DateUtil.addSeconds(actualPosition, timeGridInMinutes * 60);
			}
		}
		return foundFreeTimeWindows;
	}

	public AppointmentProposal getAppointmentProposal(Date from, Date to, List<TreatmentType> treatmentTypes) {
		if (!canBeCombinedIntoOneAppointment(treatmentTypes)) {
			return null;
		}

		if (!sufficientAppointmentDuration(from, to, treatmentTypes)) {
			return null;
		}

		from = schedulingUtil.moveFromIfOutOfWorkTime(from, to, workTime);
		to = DateUtil.addSeconds(from, (int) schedulingUtil.getAppointmentDurationInSeconds(from, to));
		Map<ResourceType, SortedSet<Resource>> resources = getResources(from, to, treatmentTypes);

		if (resources != null) {
			return new AppointmentProposal(resources, treatmentTypes, from, to);
		}
		return null;
	}

	public TimeWindow getNextTimeWindow(Date start, Date limit, List<TreatmentType> treatmentTypes, Room room,
			long appointmentDuration) {

		List<AppointmentProposal> appointmentProposals = new ArrayList<AppointmentProposal>();

		Date actualAppointmentStart = new Date(start.getTime());
		Date actualAppointmentEnd = DateUtil.addSeconds(actualAppointmentStart, (int) appointmentDuration);
		actualAppointmentStart = schedulingUtil.moveFromIfOutOfWorkTime(actualAppointmentStart, actualAppointmentEnd,
				workTime);
		actualAppointmentEnd = DateUtil.addSeconds(actualAppointmentStart, (int) appointmentDuration);
		Date endWorkTime = workTime.getEndWorkTime(actualAppointmentStart);

		while (!actualAppointmentEnd.after(limit) && !actualAppointmentEnd.after(endWorkTime)) {
			Map<ResourceType, SortedSet<Resource>> resources = getResources(actualAppointmentStart,
					actualAppointmentEnd, treatmentTypes);

			if (resources == null || !room.isNotBusy(actualAppointmentStart, actualAppointmentEnd)) {
				break;
			}

			AppointmentProposal appointmentProposal = new AppointmentProposal(resources, treatmentTypes,
					actualAppointmentStart, actualAppointmentEnd);
			appointmentProposals.add(appointmentProposal);

			actualAppointmentStart = DateUtil.addSeconds(actualAppointmentStart, timeGridInMinutes * 60);
			actualAppointmentEnd = DateUtil.addSeconds(actualAppointmentEnd, timeGridInMinutes * 60);
		}

		Date blockStart;
		Date blockEnd;

		if (appointmentProposals.isEmpty()) {
			return null;
		}

		blockStart = appointmentProposals.get(0).getFrom();
		blockEnd = appointmentProposals.get(appointmentProposals.size() - 1).getTo();

		TimeWindow block = new TimeWindow(blockStart, blockEnd, room);

		return block;
	}

	public List<AppointmentProposal> getAppointmentProposalsForBlock(Date start, Date limit,
			List<TreatmentType> treatmentTypes, Room room, long appointmentDuration) {
		List<AppointmentProposal> appointmentProposals = new ArrayList<AppointmentProposal>();
		Date endWorkTime = workTime.getEndWorkTime(start);
		Date actualAppointmentStart = new Date(start.getTime());
		Date actualAppointmentEnd = DateUtil.addSeconds(actualAppointmentStart, (int) appointmentDuration);
		while (actualAppointmentEnd.before(limit) && actualAppointmentEnd.before(endWorkTime)) {
			Map<ResourceType, SortedSet<Resource>> resources = getResources(actualAppointmentStart,
					actualAppointmentEnd, treatmentTypes);

			if (resources == null) {
				break;
			}

			AppointmentProposal appointmentProposal = new AppointmentProposal(resources, treatmentTypes,
					actualAppointmentStart, actualAppointmentEnd);
			appointmentProposals.add(appointmentProposal);

			actualAppointmentStart = DateUtil.addSeconds(actualAppointmentStart, timeGridInMinutes * 60);
			actualAppointmentEnd = DateUtil.addSeconds(actualAppointmentEnd, timeGridInMinutes * 60);
		}
		return appointmentProposals;
	}

	public List<AppointmentProposal> getAppointmentProposals(AppointmentRequest request, SchedulingParameter param) {
		if (!canBeCombinedIntoOneAppointment(request.getTreatmentTypes())) {
			setMessage(TREATMENTS_CANNOT_BE_JOINED_MSG);
			return null;
		}

		/*
		 * if (!sufficientAppointmentDuration(from, to, treatmentTypes)) {
		 * setMessage(INSUFFICIENT_DIFFERENCE_BETWEEN_FROM_AND_TO_MSG); return
		 * null; }
		 */

		
//		 SchedulingHorizon adjustedHorizon = adjustSchedulingHorizonToWorkTime(request.getHorizon());
//		 request.setHorizon(adjustedHorizon);
		 
/*		 request.setFrom(moveFromIfOutOfWorkTime(request.getFrom(), to));*/ 
//		 to = DateUtil.addSeconds(from, (int) appointmentDuration);
		 

		return policy.getAppointmentProposals(request, param);
	}

	public List<AppointmentProposal> getAppointmentProposals(Date from, Date to, List<TreatmentType> treatmentTypes,
			Date limitDate, int count) {
		if (!canBeCombinedIntoOneAppointment(treatmentTypes)) {
			setMessage(TREATMENTS_CANNOT_BE_JOINED_MSG);
			return null;
		}

		if (!sufficientAppointmentDuration(from, to, treatmentTypes)) {
			setMessage(INSUFFICIENT_DIFFERENCE_BETWEEN_FROM_AND_TO_MSG);
			return null;
		}

		long appointmentDuration = schedulingUtil.getAppointmentDurationInSeconds(from, to);
		from = schedulingUtil.moveFromIfOutOfWorkTime(from, to, workTime);
		to = DateUtil.addSeconds(from, (int) appointmentDuration);

		int proposedAppointments = 0;
		List<AppointmentProposal> appointmentProposals = new ArrayList<AppointmentProposal>();

		while (proposedAppointments < count && to.before(limitDate)) {
			Map<ResourceType, SortedSet<Resource>> resources = getResources(from, to, treatmentTypes);

			if (resources != null) {
				AppointmentProposal appointmentProposal = new AppointmentProposal(resources, treatmentTypes, from, to);
				appointmentProposals.add(appointmentProposal);
				proposedAppointments++;
			}

			from = DateUtil.addSeconds(from, timeGridInMinutes * 60);
			to = DateUtil.addSeconds(to, timeGridInMinutes * 60);
			from = schedulingUtil.moveFromIfOutOfWorkTime(from, to, workTime);
			to = DateUtil.addSeconds(from, (int) appointmentDuration);
		}
		return appointmentProposals;
	}

	public List<AppointmentProposal> getAppointmentProposals(Date from, Date to, List<TreatmentType> treatmentTypes,
			int count) {
		Date limitDate = DateUtil.addDays(DateUtil.now(), HealthPoint.SCHEDULING_HORIZON_IN_DAYS);
		return getAppointmentProposals(from, to, treatmentTypes, limitDate, count);
	}

	private List<Room> findPossibleRoomsForTreatmentTypes(List<TreatmentType> treatmentTypes) {
		List<Room> foundRooms = new ArrayList<Room>();
		TreatmentType treatmentType = treatmentTypes.get(0);
		List<RoomType> roomTypes = treatmentType.getPossibleRoomTypes();
		List<RoomType> roomTypesCopy = new ArrayList<RoomType>(roomTypes);

		// find suitable rooms (each such a room that each treatment can be
		// executed in)
		for (int i = 1; i < treatmentTypes.size(); i++) {
			TreatmentType treatmentType2 = treatmentTypes.get(i);
			for (int j = 0; j < roomTypesCopy.size(); j++) {
				if (!roomTypesCopy.get(j).getPossibleTreatmentTypes().contains(treatmentType2)) {
					RoomType roomType = roomTypesCopy.get(j);
					roomTypes.remove(roomType);
				}
			}
		}
		for (RoomType roomType : roomTypes) {
			foundRooms.add(roomType.getRoom());
		}
		return foundRooms;
	}

	private SortedSet<Resource> findSuitableRooms(List<TreatmentType> treatmentTypes, Date from, Date to) {
		TreatmentType treatmentType = treatmentTypes.get(0);
		List<RoomType> roomTypes = treatmentType.getPossibleRoomTypes();
		List<RoomType> roomTypesCopy = new ArrayList<RoomType>(roomTypes);

		// find suitable rooms (each such a room that each treatment can be
		// executed in)
		for (int i = 1; i < treatmentTypes.size(); i++) {
			TreatmentType treatmentType2 = treatmentTypes.get(i);
			for (int j = 0; j < roomTypesCopy.size(); j++) {
				if (!roomTypesCopy.get(j).getPossibleTreatmentTypes().contains(treatmentType2)) {
					RoomType roomType = roomTypesCopy.get(j);
					roomTypes.remove(roomType);
				}
			}
		}
		// create ordered set from suitable rooms that are available
		TreeSet<Resource> rooms = new TreeSet(new RoomSuitabilityComparator());
		if (roomTypes.size() > 0) {
			for (RoomType roomType : roomTypes) {
				Room room = roomType.getRoom();
				if (room.isNotBusy(from, to)) {
					rooms.add(roomType.getRoom());
				}
			}
		}
		return rooms;
	}

	private void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public boolean canBeCombinedIntoOneAppointment(List<TreatmentType> treatmentTypes) {
		if (treatmentTypes.size() < 2) {
			return true;
		}
		List<ResourceType> resourceTypes = treatmentTypes.get(0).getResourceTypes();
		for (int i = 1; i < treatmentTypes.size(); i++) {
			TreatmentType treatmentType = treatmentTypes.get(i);
			if (resourceTypes.size() != treatmentType.getResourceTypes().size()) {
				return false;
			} else {
				boolean areTheSame = resourceTypes.containsAll(treatmentType.getResourceTypes());
				if (!areTheSame) {
					return false;
				}
			}
		}
		return true;
	}

	private SortedSet<Resource> findSuitableResources(List<? extends ResourceImpl> resources, ResourceType resourceType,
			Date from, Date to, Comparator<? extends ResourceImpl> comparator) {
		SortedSet<Resource> suitableResources = new TreeSet(comparator);

		for (Resource resource : resources) {
			if (resource.isSuitable(resourceType)) {
				if (resource.isNotBusy(from, to)) {
					suitableResources.add(resource);
				}
			}
		}
		return suitableResources;
	}

	private Map<ResourceType, SortedSet<Resource>> getResources(Date from, Date to, List<TreatmentType> treatmentTypes,
			Room room) {
		Map<ResourceType, SortedSet<Resource>> resources = new HashMap<ResourceType, SortedSet<Resource>>();
		List<Physician> physicians = physicianDao.getAll();
		List<Nurse> nurses = nurseDao.getAll();
		List<Device> devices = deviceDao.getAll();
		List<ResourceType> neededResourceTypes = treatmentTypes.get(0).getResourceTypes();

		for (ResourceType neededResourceType : neededResourceTypes) {
			if (neededResourceType instanceof PhysicianType) {
				SortedSet<Resource> suitablePhysicians = findSuitableResources(physicians, neededResourceType, from, to,
						new PhysicianSuitabilityComparator());
				if (suitablePhysicians.isEmpty()) {
					return null;
				}
				resources.put(neededResourceType, suitablePhysicians);

			} else if (neededResourceType instanceof NurseType) {
				SortedSet<Resource> suitableNurses = findSuitableResources(nurses, neededResourceType, from, to,
						new NurseSuitabilityComparator());
				if (suitableNurses.isEmpty()) {
					return null;
				}
				resources.put(neededResourceType, suitableNurses);

			} else if (neededResourceType instanceof DeviceType) {
				SortedSet<Resource> suitableDevices = findSuitableResources(devices, neededResourceType, from, to,
						new DeviceSuitabilityComparator());
				if (suitableDevices.isEmpty()) {
					return null;
				}
				resources.put(neededResourceType, suitableDevices);
			}
		}
		return resources;
	}

	private Map<ResourceType, SortedSet<Resource>> getResources(Date from, Date to,
			List<TreatmentType> treatmentTypes) {
		Map<ResourceType, SortedSet<Resource>> resources = new HashMap<ResourceType, SortedSet<Resource>>();
		List<Physician> physicians = physicianDao.getAll();
		List<Nurse> nurses = nurseDao.getAll();
		List<Device> devices = deviceDao.getAll();

		List<ResourceType> neededResourceTypes = treatmentTypes.get(0).getResourceTypes();

		for (ResourceType neededResourceType : neededResourceTypes) {
			if (neededResourceType instanceof PhysicianType) {
				SortedSet<Resource> suitablePhysicians = findSuitableResources(physicians, neededResourceType, from, to,
						new PhysicianSuitabilityComparator());
				if (suitablePhysicians.isEmpty()) {
					return null;
				}
				resources.put(neededResourceType, suitablePhysicians);

			} else if (neededResourceType instanceof NurseType) {
				SortedSet<Resource> suitableNurses = findSuitableResources(nurses, neededResourceType, from, to,
						new NurseSuitabilityComparator());
				if (suitableNurses.isEmpty()) {
					return null;
				}
				resources.put(neededResourceType, suitableNurses);

			} else if (neededResourceType instanceof RoomType) {
				SortedSet<Resource> suitableRooms = findSuitableRooms(treatmentTypes, from, to);
				if (suitableRooms.isEmpty()) {
					return null;
				}
				resources.put(neededResourceType, suitableRooms);
			} else if (neededResourceType instanceof DeviceType) {
				SortedSet<Resource> suitableDevices = findSuitableResources(devices, neededResourceType, from, to,
						new DeviceSuitabilityComparator());
				if (suitableDevices.isEmpty()) {
					return null;
				}
				resources.put(neededResourceType, suitableDevices);
			}
		}
		return resources;
	}
}
