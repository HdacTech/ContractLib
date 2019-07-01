Hdac ContractLib
======================

ContractLib
-----------
The released ContractLib is a lib that has been written according to the method in ContractLib. 
Currently, the swap method is registered, and the structure supports automatic refund when it is not a swap method.
The method calls the Hdac API as a Java method with using HdacJavaLib file included in the method.
Please check the source code of [BridgeNode-ContractService](https://github.com/Hdactech/BridgeNode-ContractService) to see how it works.


### License

Hdac contractlib is licensed under the [MIT License](http://opensource.org/licenses/MIT).

Copyright (c) 2018-2019 Hdac Technology AG  


Development environment
-----------------------
>- JavaSE 1.8 optimization
>- Using Eclipse Oxygen.1a Release (4.7.1a)
>- Use HdacJavaLib.jar,
>- This jar file must be included in the project.


Maven
-----
See pom.xml


Getting Started
---------------
>1. Download the source code and add the project through Eclipse.
>2. File > Import > Maven | Existing Maven Projects
>3. Choose the folder where the source is located, check pom.xml, and complete the import.
>4. After Project> Clean, Run Build Project.


_A detailed description of the docker setting and operation can be found here [Bridgenode docker hub](https://hub.docker.com/r/hdac/bridgenode)._
