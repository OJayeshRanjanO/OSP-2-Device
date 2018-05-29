package osp.Devices;

/**
    This class stores all pertinent information about a device in
    the device table.  This class should be sub-classed by all
    device classes, such as the Disk class.

    @OSPProject Devices
*/

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;

public class Device extends IflDevice
{
    /**
        This constructor initializes a device with the provided parameters.
	As a first statement it must have the following:

	    super(id,numberOfBlocks);

	@param numberOfBlocks -- number of blocks on device

        @OSPProject Devices
    */
    public Device(int id, int numberOfBlocks)
    {
        // your code goes here
        super(id,numberOfBlocks);
        iorbQueue = new GenericList();

    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Devices
    */
    public static void init()
    {
        // your code goes here

    }

    /**
       Enqueues the IORB to the IORB queue for this device
       according to some kind of scheduling algorithm.
       
       This method must lock the page (which may trigger a page fault),
       check the device's state and call startIO() if the 
       device is idle, otherwise append the IORB to the IORB queue.

       @return SUCCESS or FAILURE.
       FAILURE is returned if the IORB wasn't enqueued 
       (for instance, locking the page fails or thread is killed).
       SUCCESS is returned if the IORB is fine and either the page was 
       valid and device started on the IORB immediately or the IORB
       was successfully enqueued (possibly after causing pagefault pagefault)
       
       @OSPProject Devices
    */
    public int do_enqueueIORB(IORB iorb)
    {
        // your code goes here
        //First
        PageTableEntry page = iorb.getPage();
        page.lock(iorb);
        GenericList list = (GenericList)iorbQueue;

        //Second
        iorb.getOpenFile().incrementIORBCount();

        int bytesPerBlock = ((int) Math.pow(2,  (MMU.getVirtualAddressBits() - MMU.getPageAddressBits())));//Size of each block in bytes
        int blocksPerSector = bytesPerBlock/((Disk)this).getBytesPerSector();//Sectors per block
        int blocksPerTrack = ((Disk)this).getSectorsPerTrack()/blocksPerSector;
        int blocksPerCylinder = blocksPerTrack * ((Disk)this).getPlatters();
        int currentCylinder = (int)(iorb.getBlockNumber()/blocksPerCylinder);

        iorb.setCylinder(currentCylinder);

        //Check if the thread is alive
        if (iorb.getThread().getStatus() == ThreadKill){//If the thread has died then return failure
        	return FAILURE;
        }else{
        	// Device device = iorb.get(iorb.getID());
        	if (!isBusy()){//If the device is not busy
        		startIO(iorb);
        		return SUCCESS;
        	}else{
        		list.append(iorb);//NEEDS TO BE SORTED
        		return SUCCESS;
        	}
        }
    }

    /**
       Selects an IORB (according to some scheduling strategy)
       and dequeues it from the IORB queue.

       @OSPProject Devices
    */
    public IORB do_dequeueIORB()
    {
        // your code goes here
        GenericList list = (GenericList)iorbQueue;
        if (iorbQueue.isEmpty()){
        	return null;
        }else{
        	return (IORB)list.removeHead();//removes first 
        }
    }

    /**
        Remove all IORBs that belong to the given ThreadCB from 
	this device's IORB queue

        The method is called when the thread dies and the I/O 
        operations it requested are no longer necessary. The memory 
        page used by the IORB must be unlocked and the IORB count for 
	the IORB's file must be decremented.

	@param thread thread whose I/O is being canceled

        @OSPProject Devices
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
        // your code goes here
        GenericList list = (GenericList)iorbQueue;
        // MyOut.print(thread,"----"+list.length()+"------");
        if (thread.getStatus() == ThreadKill){
        	Enumeration iterator = list.forwardIterator();
			while(iterator.hasMoreElements()) {
				IORB iorb = (IORB)(iterator.nextElement());
				// IORB iorb = (IORB)obj;
				if (iorb.getThread() == thread){
					iorb.getPage().unlock();
					iorb.getOpenFile().decrementIORBCount();
					if (iorb.getOpenFile().getIORBCount()==0 && iorb.getOpenFile().closePending){
						iorb.getOpenFile().close();
					}
					list.remove(iorb);

				}
			}

			//Remove all the iorbs requests 
			// while(iterator.hasMoreElements()) {
			// 	IORB iorb = (IORB)(iterator.nextElement());
			// 	if (iorb.getThread() == thread){
			// 		list.remove(iorb);
			// 	}
			// }

        }
    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
