import java.lang.Math;
import java.util.ArrayList;

public class IpBean {
    private long ipAddr = 0;

    private int mask = 0;

    private int sIPAddrLong = 0;

    private String sIPAddress;

    private int firstIPAddr=0;

    private int secondIPAddr=0;

    private int thirdIPAddr=0;

    private int fourIPAddr=0;

    public IpBean(String ipAddrString, int mask){
        this.mask = mask;
        this.sIPAddress = ipAddrString;
        String[] ipSegList = ipAddrString.split("\\.");
        for(int i=0;i<ipSegList.length;i++)
        {
            String ipSeg=ipSegList[i];
            int segValue = Integer.parseInt(ipSeg, 10);
            if (segValue >= 256)
                throw new RuntimeException("该IP地址有误");
            this.ipAddr = this.ipAddr * 256 + segValue;

        }
        this.firstIPAddr=Integer.parseInt(ipSegList[0],10);
        this.secondIPAddr=Integer.parseInt(ipSegList[1],10);
        this.thirdIPAddr=Integer.parseInt(ipSegList[2],10);
        this.fourIPAddr=Integer.parseInt(ipSegList[3],10);
        if (mask > 32)
        {
            throw new RuntimeException("IP地址段的掩码有误[" + toString() + "]");
        }
    }

    private String concactIpString(int IPAddrs[], int mask){
        StringBuilder ipString = new StringBuilder();
        for (int element : IPAddrs)
        {
            ipString.append(element);
            ipString.append('.');
        }
        ipString.deleteCharAt(ipString.length()-1);
        return ipString.toString();
    }

    public long getIpAddr(){return this.ipAddr;}

    public  int getFirstIPAddr() {return this.firstIPAddr;}

    public  int getSecondIPAddr() {return this.secondIPAddr;}

    public  int getThirdIPAddr() {return this.thirdIPAddr;}

    public  int getFourIPAddr() {return this.fourIPAddr;}

    public  int getMask() {return this.mask;}

    public  String getsIPAddress() {return this.sIPAddress;}

    public  long getsIPAddrLong() {return this.sIPAddrLong;}

    @Override
    public String toString() {
        return this.getsIPAddress().concat("/").concat(String.valueOf(this.getMask()));
    }

    public ArrayList<IpBean> splitArray2List(IpBean [] array) {
        ArrayList<IpBean> theSplitIps = new ArrayList<IpBean>();
        for (IpBean e : array) {
            if(e != null) {
                theSplitIps.add(e);
            }
        }
        return theSplitIps;
    }

    public void copyList2Array(ArrayList<IpBean> list, IpBean [] array, int target) {
        for(int index = 0; index < list.size(); index ++)
        {
            array[target++] =  list.get(index);
        }
    }

    public  ArrayList<IpBean> ipSplit_By_Two(int splitStep, int splitMask,int IPAddrs[]) {
        int newIPFourAddr1 = IPAddrs[3];
        int newIPFourAddr2 = newIPFourAddr1 + splitStep;
        int [] newIpSegs1 = {IPAddrs[0],IPAddrs[1],IPAddrs[2],newIPFourAddr1};
        int [] newIpSegs2 = {IPAddrs[0],IPAddrs[1],IPAddrs[2],newIPFourAddr2};
        ArrayList<IpBean> returnIps = new ArrayList<IpBean>();
        returnIps.add(new IpBean(concactIpString(newIpSegs1,splitMask), splitMask));
        returnIps.add(new IpBean(concactIpString(newIpSegs2,splitMask), splitMask));
        return returnIps;
    }

    public ArrayList<IpBean> cIPSplit(int descMask, int descCount) {
        int realMaxCount = (int) Math.pow(2, descMask - this.mask);
        descCount = descCount > realMaxCount ? realMaxCount : descCount;
        IpBean[] resultIps = new IpBean[realMaxCount];
        resultIps[0] = this;
        int layerStartIndex = 0, layerEndIndex = 1;
        for (int depth = 0; depth < descMask - this.mask; depth++) {
            //At the (depth) level, the range of index that the resultIps array needs to be split is (layerStartIndex) to (layerEndIndex)
            int theLayerCount = (layerEndIndex - layerStartIndex) * 2;
            int theNodeMask = this.mask + depth + 1;
            int theNodeValue = (int) Math.pow(2, descMask - this.mask - depth);
            ArrayList<IpBean> theLayerSplitIps = new ArrayList<IpBean>();
            //At the (depth) level, there are (theLayerCount) nodes, each node can be split into (theNodeValue) /(descMask) IPsegment
            for (int theLayerNodes = 0; theLayerNodes < layerEndIndex - layerStartIndex; theLayerNodes++) {
                boolean needSplitTheNode = theNodeValue * theLayerNodes < descCount ? true : false;
                int index = layerStartIndex + theLayerNodes;
                if (needSplitTheNode) {
                    //The (depth) level node has been split into (2*theLayerNodes) children IPsegment
                    int splitStep = (int) Math.pow(2, 32 - theNodeMask);
                    int nodeIPAddrs[] = {resultIps[index].getFirstIPAddr(), resultIps[index].getSecondIPAddr(), resultIps[index].getThirdIPAddr(), resultIps[index].getFourIPAddr()};
                    theLayerSplitIps.addAll(ipSplit_By_Two(splitStep, theNodeMask, nodeIPAddrs));
                } else {
                    //From 0-level to the (depth) level, (layerStartIndex) nodes have not been split
                    resultIps[layerStartIndex++] = resultIps[index];
                }
                resultIps[index] = null;
            }
            copyList2Array(theLayerSplitIps, resultIps, layerStartIndex);
            layerEndIndex = layerStartIndex + theLayerSplitIps.size();
        }
        ArrayList<IpBean> theSplitIps = splitArray2List(resultIps);
        return theSplitIps;
    }

    public static void main(String[] args) {
        IpBean aip = new  IpBean("192.168.7.0",24);
        System.out.println(aip.toString() + "----进行C类地址拆分的结果为：");
        //拆分规则的, 目标IP地址子网掩码长度, IP地址个数
        int descMask = 28;
        int descCount = 8;
        int realMaxCount =  (int)Math.pow(2, descMask-24);
        //调用cIPSplit函数，进行C类IP地址拆分
        ArrayList<IpBean> result = aip.cIPSplit(descMask,descCount);
        if (descCount < realMaxCount) {
            int printNeed = 0;
            System.out.println("----剩余IP地址----");
            for(int i = 0; i < result.size(); i++) {
                if (result.get(i).getMask() != descMask) {
                    System.out.println(result.get(i).toString());
                } else {
                    if (printNeed++ == 0) {
                        System.out.println("----目标IP地址----");
                    } else if (printNeed > descCount){
                        System.out.println("----剩余IP地址----");
                    }
                    System.out.println(result.get(i).toString());
                }
            }
        } else {
            System.out.println("----目标IP地址----");
            for(int i = 0; i < result.size(); i++) {
                System.out.println(result.get(i).toString());
                if (i == (int) Math.pow(2, descMask - 24) - descCount) {
                }
            }
        }
    }
}
