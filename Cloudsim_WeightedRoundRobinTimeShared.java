/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package com.mycompany.weightedroundrobin;


import java.text.DecimalFormat;
import java.util.*;
import java.lang.*;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.*;

public class Cloudsim_WeightedRoundRobinTimeShared {
	private static List<Cloudlet> cloudletList;
	private static List<Vm> vmlist;

        public static HashMap<Integer, Double> sortByValue(HashMap<Integer, Double> hm)
        {
            List<Map.Entry<Integer, Double> > list = new LinkedList<Map.Entry<Integer, Double> >(hm.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<Integer, Double> >() {
                public int compare(Map.Entry<Integer, Double> o1,
                                   Map.Entry<Integer, Double> o2)
                {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
            });
            HashMap<Integer, Double> temp = new LinkedHashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> aa : list) {
                temp.put(aa.getKey(), aa.getValue());
            }
            return temp;
        }
	public static void main(String[] args) {

		Log.printLine("Starting WRR scheduling...");

	        try {
	            	int num_user = 1;   // number of cloud users
	            	Calendar calendar = Calendar.getInstance();
	            	boolean trace_flag = false;  // mean trace events

	            	CloudSim.init(num_user, calendar, trace_flag);
                        @SuppressWarnings("unused")
					Datacenter datacenter0 = createDatacenter("Datacenter_0");

	            	DatacenterBroker broker = createBroker();
	            	int brokerId = broker.getId();

	            	vmlist = new ArrayList<Vm>();
	            	int vmid=0;
	            	int mips[] = {53,52,50,57,51,54,59,55,58,56}; 
	            	long size = 10000; //image size (MB)
	            	int ram = 512; //vm memory (MB)
	            	long bw = 1000;
                        int pesNumber = 1; //number of CPUs
	            	String vmm = "Xen"; //VMM name
                        for(int i=0;i<10;i++){
                            vmlist.add(new Vm(vmid, brokerId, mips[i], pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared()));
                            vmid++;
                        }
	 
	            	cloudletList = new ArrayList<Cloudlet>();
	            	int id = 0;
	            	long length = 250000;
	            	long fileSize = 300;
	            	long outputSize = 300;
	            	UtilizationModel utilizationModel = new UtilizationModelFull();
                        Cloudlet arr[] = new Cloudlet[100];
                        for(int i=0;i<100;i++){
                            arr[i] = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                            arr[i].setUserId(brokerId);
                            cloudletList.add(arr[i]);
                            id++;
                        }
	            	broker.submitCloudletList(cloudletList);

                        HashMap<Integer, Double> map = new HashMap<>(); 
                        for(int i=0;i<10;i++){
                            double check = vmlist.get(i).getMips()*vmlist.get(i).getNumberOfPes();
                            map.put(i, check);
                        }
                        HashMap<Integer, Double> sortlist = sortByValue(map);
                        
                        vmlist.clear();
                        int v=0;
                        for (Map.Entry<Integer, Double> en : sortlist.entrySet())
                        { 
                            vmlist.add(new Vm(en.getKey(), brokerId, mips[v], pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared()));
                            v++;
                        }
                        broker.submitVmList(vmlist);
                        
                        for(int i=0;i<100;i++){ 
                            int j = i%vmlist.size();
                            broker.bindCloudletToVm(cloudletList.get(i).getCloudletId(),vmlist.get(j).getId());
                        }
	            	CloudSim.startSimulation();
	            	List<Cloudlet> newList = broker.getCloudletReceivedList();
	            	CloudSim.stopSimulation();
	            	printCloudletList(newList);
	            	Log.printLine("WRR scheduling is finished!");
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	            Log.printLine("The simulation has been terminated due to an unexpected error");
	        }
	    }

		private static Datacenter createDatacenter(String name){
	    	List<Host> hostList = new ArrayList<Host>();
	    	List<Pe> peList = new ArrayList<Pe>();
	    	int mips = 1000;
	    	peList.add(new Pe(0, new PeProvisionerSimple(mips)));
	        int hostId=0;
	        int ram = 8192; //host memory (MB)
	        long storage = 1000000; //host storage
	        int bw = 10000;

	        hostList.add(
	    			new Host(
	    				hostId,
	    				new RamProvisionerSimple(ram),
	    				new BwProvisionerSimple(bw),
	    				storage,
	    				peList,
	    				new VmSchedulerTimeShared(peList)
	    			)
	    		);
	        String arch = "x86";      // system architecture
	        String os = "Linux";          // operating system
	        String vmm = "Xen";
	        double time_zone = 10.0;         // time zone this resource located
	        double cost = 3.0;              // the cost of using processing in this resource
	        double costPerMem = 0.05;		// the cost of using memory in this resource
	        double costPerStorage = 0.001;	// the cost of using storage in this resource
	        double costPerBw = 0.0;			// the cost of using bw in this resource
	        LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

	        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);
	        Datacenter datacenter = null;
	        try {
	            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
	        } 
                catch (Exception e) {
	            e.printStackTrace();
	        }

	        return datacenter;
	    }
	    private static DatacenterBroker createBroker(){

	    	DatacenterBroker broker = null;
	        try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	    	return broker;
	    }

	    private static void printCloudletList(List<Cloudlet> list) {
	        int size = list.size();
	        Cloudlet cloudlet;

	        String indent = "    ";
	        Log.printLine();
	        Log.printLine("========== OUTPUT ==========");
	        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
	                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

	        DecimalFormat dft = new DecimalFormat("###.##");
	        for (int i = 0; i < size; i++) {
	            cloudlet = list.get(i);
	            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

	            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
	                Log.print("SUCCESS");

	            	Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
	                     indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
                             indent + indent + dft.format(cloudlet.getFinishTime()));
	            }
	        }

	    }
}
