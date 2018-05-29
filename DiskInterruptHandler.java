package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
    The disk interrupt handler.  When a disk I/O interrupt occurs,
    this class is called upon the handle the interrupt.

    @OSPProject Devices
*/
public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
        
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
        // your code goes here
        //STEP 1
        // InterruptVector.setInterrupt(DiskInterrupt);
        IORB iorb = (IORB)InterruptVector.getEvent();

        //STEP 2
        OpenFile openFile = iorb.getOpenFile();
        openFile.decrementIORBCount();

        //STEP 3
        if (openFile.closePending && iorb.getOpenFile().getIORBCount()==0){
        	openFile.close();
        }

        //STEP 4
        iorb.getPage().unlock();

        //STEP 5
        if(iorb.getDeviceID() != SwapDeviceID){//If the IO operation is not a page swap in or swap out then..
        	if (iorb.getThread().getStatus() != ThreadKill){//The thread that created the IORB is not dead
        		iorb.getPage().getFrame().setReferenced(true);//Set the frame associated as referenced
        		if (iorb.getIOType()==FileRead){//Check if he IO is Read type, then 
        			if (iorb.getThread().getTask().getStatus() == TaskLive){//Check if the task associated with the thread of the IO is still alive
        				iorb.getPage().getFrame().setDirty(true);//If so then set the frame assocated with the thread as dirty
        			}
        		}
        	}
        }

        //STEP 6
        if(iorb.getDeviceID() == SwapDeviceID){//If the IO operation is a page swap in or swap out then..{//STEP 6
	        if (iorb.getThread().getTask().getStatus() == TaskLive){//Check if the task associated with the thread of the IO is still alive
				iorb.getPage().getFrame().setDirty(false);//If so then set the frame assocated with the thread as not dirty
			}
        }

        //STEP 7
       	if (iorb.getThread().getTask().getStatus() == TaskTerm){//Check if the task associated with the thread of the IO is dead
       		// FrameTableEntry frame = iorb.getPage().getFrame();
       		// frame.isReserved();
       		if(iorb.getThread().getTask() == iorb.getPage().getFrame().getReserved()){//If the frame is reserved
				iorb.getPage().getFrame().setUnreserved(iorb.getThread().getTask());//Unreserve the frame of the task
       		}
		}

		//STEP 8
		iorb.notifyThreads();

		//STEP 9
		Device.get(iorb.getDeviceID()).setBusy(false);

		//STEP 10
		IORB newIOService = Device.get(iorb.getDeviceID()).dequeueIORB();
		if (newIOService!=null){
			Device.get(newIOService.getDeviceID()).startIO(newIOService);
		}

		//STEP 11
		iorb.getThread().dispatch();



        

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
