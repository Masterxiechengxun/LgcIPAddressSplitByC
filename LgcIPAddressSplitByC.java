/**
 * Description:
 *  C类地址拆分的算法策咯类
 * @author xie.chengxun
 * @date 2018/4/2.
 */
public class LgcIPClassCAddressSplit extends LgcIPSplitRule {

    /**
     * Description
     *   按照C类地址拆分规则，计算下级IP地址
     * @param lowerResList 下级IP地址对应的操作资源实体
     * @author xie.chengxun
     * @date 2018年4月3日
     */
    @Override
    protected void computeSubIpList(List<Entity> lowerResList) {
        List<IPAddress> subIpAddressList =  new ArrayList<IPAddress>();
        simulateBinaryTreeSplit(subIpAddressList);
        //subIpAddressList中有值为null的元素
        subIpAddressList.removeAll(Collections.singleton(null));
        adjustFreeIP2Tail(subIpAddressList);
        //C类地址拆分，拆分生成的下级IP地址实际个数
        int realSubIpNum = subIpAddressList.size();
        //预估的下级IP地址待拆分数量
        int estimateSubIpNum = lowerResList.size();
        for (int subIpNo = 0; subIpNo < estimateSubIpNum; subIpNo++) {
            if (subIpNo < realSubIpNum) {
                Entity entity = lowerResList.get(subIpNo);
                IPAddress subIpAddress = subIpAddressList.get(subIpNo);
                updateSubEntityProperties(entity, subIpAddress);
            } else {
                //将lowerResList中按照预估数量创建的多余实体元素删除
                //每次都删除lowerResList的最后一个元素，调用次数等于多余元素个数
                lowerResList.remove(lowerResList.size() - 1);
            }
        }
    }

    /**
     * Description
     *   C类地址拆分规则的算法实现：
     *      二叉树的逐层增长，层数等于32-24+1 ---> 模拟从/24地址逐级拆分，每级拆分掩码长度增一，直到等于目标掩码长度
     *      赋值每层二叉树的所有节点一个共同值 ---> 表示当层每个节点对应的地址可以完全拆分出多少个目标掩码长度的地址
     *      二叉树的每层节点有两棵或者零棵子树 ---> 判断某个节点对应的地址是否需要被拆分，通过求和当前层已拆分的节点的值判断
     * @param nodeIpAddressList 用于添加拆分后的下级IP地址对象的列表
     * @author xie.chengxun
     * @date 2018年4月3日
     */
    private void simulateBinaryTreeSplit(List<IPAddress> nodeIpAddressList) {
        IPAddress rootIPAddress = new IPAddress(minusIpSegmFour(getParentLongAddress()), CLASS_C_MASK_VALUE);
        nodeIpAddressList.add(0,rootIPAddress);
        for (int depth = 0, layerStartIndex = 0, layerEndIndex = 1; depth < getTargetMask() - getParentMask(); depth++) {
            int subNodeMask = getParentMask() + depth + 1;
            int subNodeValue = (int) Math.pow(2, getTargetMask() - getParentMask() - depth);
            ArrayList<IPAddress> thisLayerSplitIps = new ArrayList<IPAddress>();
            for (int thisLayerNode = 0; thisLayerNode < layerEndIndex - layerStartIndex; thisLayerNode++) {
                boolean needSplitThisNode = thisLayerNode * subNodeValue < targetSubIPNumber();
                int index = layerStartIndex + thisLayerNode;
                if (needSplitThisNode) {
                    int splitStep = computeStep(subNodeMask, MAX_MASK_VALUE);
                    thisLayerSplitIps.addAll(ipHalfSplitByStep(splitStep, subNodeMask, nodeIpAddressList.get(index)));
                } else {
                    nodeIpAddressList.set(layerStartIndex++, nodeIpAddressList.get(index));
                }
                nodeIpAddressList.set(index, null);
            }
            nodeIpAddressList.addAll(layerStartIndex, thisLayerSplitIps);
            layerEndIndex = layerStartIndex + thisLayerSplitIps.size();
        }
    }

    /**
     * Description
     * 将传入的IP地址对象节点，按照步长和掩码拆分成两个IP地址子节点
     * @param splitStep 拆分的步长
     * @param splitMask 拆分的目标掩码
     * @param nodeIpAddress 被拆分的地址节点
     * @author xie.chengxun
     * @date 2018年4月4日
     */
    private ArrayList<IPAddress> ipHalfSplitByStep(int splitStep, int splitMask, IPAddress nodeIpAddress) {
        ArrayList<IPAddress> splitIpAddress = new ArrayList<IPAddress>();
        splitIpAddress.add(new IPAddress(nodeIpAddress.getLongAddress(), splitMask));
        splitIpAddress.add(new IPAddress(nodeIpAddress.getLongAddress() + splitStep, splitMask));
        return splitIpAddress;
    }

    /**
     * Description
     * 将IP对象列表首尾部的拆分剩余IP地址统一移动到列表尾部
     * @param ipAddressList 拆分后生成的下级IP地址对象列表
     * @author xie.chengxun
     * @date 2018年4月4日
     */
    private void adjustFreeIP2Tail(List<IPAddress> ipAddressList) {
        int  count = ipAddressList.size();
        int bound = 0, destMask = getTargetMask();
        for (int index = 0; index < count; index++) {
            if (ipAddressList.get(index).getMaskValue() == destMask) {
                bound = index;
                break;
            }
        }
        ArrayList<IPAddress> sortList = new ArrayList<IPAddress>();
        sortList.addAll(0,ipAddressList.subList(bound, count));
        sortList.addAll(count-bound,ipAddressList.subList(0, bound));
        ipAddressList.clear();
        ipAddressList.addAll(sortList);
    }

    final int computeStep(int startMask, int endMask) {
        return (int) Math.pow(2, endMask - startMask);
    }

    /**
     * Description
     * 计算下级IP地址待拆分数量
     * 当输入的拆分子地址目标个数大于当前地址最多可拆分的子地址数，取值已当前地址最多可拆分的子地址数
     *
     * @return 下级IP地址待拆分数量
     * @author xie.chengxun
     * @date 2018年4月2日
     */
    protected int targetSubIPNumber() {
    }

    /**
     * Description
     * 从父级IP地址的十进制表示中减去C类地址应该不计算的编码第四位
     *
     * @return 父级IP地址的十进制表示
     * @author xie.chengxun
     * @date 2018年4月17日
     */
    protected long minusIpSegmFour(long ipLong) {
    }

    
}
