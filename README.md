FreeStore V0
----------

Quorum systems are useful tools for implementing consistent and available storage in the presence of failures. These systems usually comprise a static set of servers that provide a fault-tolerant read/write register accessed by a set of clients. 

FreeStore is a dynamic variant of these systems and consists of a set of fault-tolerant protocols that emulates a register in dynamic asynchronous systems in which processes are able to join/leave the servers set at runtime. These protocols use a new abstraction called view generators, that captures the agreement requirements of reconfiguration and can be implemented in different system models with different properties, i.e., Freestore protocols are tunable to execute consensus-based or consensus-free reconfigurations. Consequently, the reconfiguration protocol that is modular, efficient, tunable (consensus-free or consesus-based) and loosely coupled with read/write protocols, improving the overall system performance.


------ How to run FreeStore -----------------------

To be completed



------ Additional information and publications ------

  - Papers describing FreeStore protocols: 
      - https://arxiv.org/pdf/1607.05344 (published at Arxiv 2016)
      - http://link.springer.com/chapter/10.1007%2F978-3-642-33651-5_49 (Brief Announcement at DISC 2012)
      - http://ieeexplore.ieee.org/document/6927130/ (portuguese -- published at SBRC 2014)
   
  - Paper describing Mateus Braga GoLang implementation of FreeStore: 
      - http://sbrc2014.ufsc.br/anais/files/wtf/ST2-4.pdf (portuguese -- published at WTF 2014)
      - http://bdm.unb.br/bitstream/10483/8130/1/2014_MateusAntunesBraga.pdf (portuguese -- Mateus Braga's Bachelor's thesis 2014)


------ Other Implementations ------

  - Mateus Braga GoLang implementation of FreeStore: https://github.com/mateusbraga/freestore 

Feel free to contact us if you have any questions!
