package com.ezticket.web.activity.service;

import com.ezticket.web.activity.dto.PlaceToBlockModelDTO;
import com.ezticket.web.activity.dto.SessionDto;
import com.ezticket.web.activity.pojo.*;
import com.ezticket.web.activity.repository.SeatsRedisDAO;
import com.ezticket.web.activity.repository.SeatsRepository;
import com.ezticket.web.activity.repository.SessionRepository;
import com.ezticket.web.activity.repository.impl.BlockModelDAOImpl;
import com.ezticket.web.activity.repository.impl.SeatsModelDAOImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SeatsService {

    private SeatsModelDAOImpl seatsModelDAOImpl = new SeatsModelDAOImpl();
    private BlockModelDAOImpl blockModelDAOImpl = new BlockModelDAOImpl();

    @Autowired
    private SeatsRepository seatsRepository;

    @Autowired
    private SeatsRedisDAO seatsRedisDAO;

    @Autowired
    private BlockModelService blockModelService;

    @Autowired
    private BlockPriceService blockPriceService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public List<Seats> getSeatsBySessionAndBlockNo(Integer sessionNo, Integer blockNo) {
        return seatsRepository.findBySessionNoAndBlockNo(sessionNo, blockNo);
    }

    public List<Integer> getToSellSeatsBySessionAndBlock(int sessionNo, int blockNo, int seatStatus) {
        return seatsRepository.getIdsBySessionNoANDBlockNoAndSeatStatus(sessionNo, blockNo, seatStatus);
    }

    public int updateOneSeat(String blockName, String realX, String realY, int seatStatus, int seatNo) {
        return seatsRepository.update(blockName, realX, realY, seatStatus, seatNo);
    }

    public boolean insertNewSeat(Seats seats) {
        seatsRepository.save(seats);
        return true;
    }

    public int updateOneSeatStatus(int seatStatus, int seatNo) {
        return seatsRepository.updateStatus(seatStatus, seatNo);
    }

    public List<Integer> getActBlockHasSeats(int actNo) {
        return seatsRepository.getActBlockHasSeats(actNo);
    }

    public boolean isSessionBlockSeatsExist(int sessionNo, int blockNo) {
        if (!seatsRepository.getSessionBlockSeatsExist(sessionNo, blockNo).isEmpty()) {
            return true;
        }
        return false;
    }

    public int setSessionSeats(int seatStatus, int seatNo, int sessionNo) {

        if (seatStatus == -1) { // 當座位變為限制座位時，將座位編號移出 Redis 中
            seatsRedisDAO.setDelKV("Session" + sessionNo, String.valueOf(seatNo));
            return 1;
        }

        if (seatStatus == 0) { // 當座位變為保留席時，將座位編號移出 Redis 中
            seatsRedisDAO.setDelKV("Session" + sessionNo, String.valueOf(seatNo));
            return 1;
        }

        if (seatStatus == 1) { // 當座位取消選取或超過結帳時間時，將座位編號移出 Redis 中
            seatsRedisDAO.setDelKV("Session" + sessionNo, String.valueOf(seatNo));
            return 1;
        }

        if (seatStatus == 2) { // 當座位結帳成功時，將座位編號移出 Redis 並修改 MySQL 座位狀態
            seatsRedisDAO.setDelKV("Session" + sessionNo, String.valueOf(seatNo));
            updateOneSeatStatus(seatStatus, seatNo);
            return 1;
        }

        if (seatStatus == 3) { // 當座位被選取時，將座位編號存入 Redis 中
            seatsRedisDAO.setAddKV("Session" + sessionNo, String.valueOf(seatNo));
            return 1;
        }

        return 0;
    }

    public List<Integer> getSeatsBySystem(int ticketQTY, int blockNo, int sessionNo) {
        List<Integer> toSellSeats = getToSellSeatsBySessionAndBlock(sessionNo, blockNo, 1);

        Set<String> lockedSeats = getLockedSeatsBySession(sessionNo);

        for (String seat : lockedSeats) {
            int seatNo = Integer.parseInt(seat);

            if (toSellSeats.contains(seatNo)) {
                toSellSeats.remove(toSellSeats.indexOf(seatNo));
            }
        }

        List<Integer> returnedList = new ArrayList<Integer>();

        try {
            switch (ticketQTY) {
                case 1:
                    returnedList.clear();
                    returnedList.add(toSellSeats.get(0));
                    break;
                case 2:
                    returnedList.clear();

                    for (int i = 0; i < toSellSeats.size() - 1; i++) {
                        returnedList.clear();
                        returnedList.add(toSellSeats.get(i));
                        returnedList.add(toSellSeats.get(i + 1));
                        if (seatsRedisDAO.setFindAllValues("Session" + sessionNo + ":Set:2").contains(returnedList.toString()) == true)
                            break;
                        if (i == (toSellSeats.size() - 1) - 1){
                            if (seatsRedisDAO.setFindAllValues("Session" + sessionNo + ":Set:2").contains(returnedList.toString()) == true)
                                break;
                            else
                                throw new Exception();
                        }
                    }
                    break;
                case 3:
                    returnedList.clear();

                    for (int i = 0; i < toSellSeats.size() - 2; i++) {
                        returnedList.clear();
                        returnedList.add(toSellSeats.get(i));
                        returnedList.add(toSellSeats.get(i + 1));
                        returnedList.add(toSellSeats.get(i + 2));
                        if (seatsRedisDAO.setFindAllValues("Session" + sessionNo + ":Set:3").contains(returnedList.toString()) == true)
                            break;
                        if (i == (toSellSeats.size() - 2) - 1){
                            if (seatsRedisDAO.setFindAllValues("Session" + sessionNo + ":Set:3").contains(returnedList.toString()) == true)
                                break;
                            else
                                throw new Exception();
                        }
                    }
                    break;
                case 4:
                    returnedList.clear();

                    for (int i = 0; i < toSellSeats.size() - 3; i++) {
                        returnedList.clear();
                        returnedList.add(toSellSeats.get(i));
                        returnedList.add(toSellSeats.get(i + 1));
                        returnedList.add(toSellSeats.get(i + 2));
                        returnedList.add(toSellSeats.get(i + 3));
                        if (seatsRedisDAO.setFindAllValues("Session" + sessionNo + ":Set:4").contains(returnedList.toString()) == true)
                            break;
                        if (i == (toSellSeats.size() - 3) - 1){
                            if (seatsRedisDAO.setFindAllValues("Session" + sessionNo + ":Set:4").contains(returnedList.toString()) == true)
                                break;
                            else
                                throw new Exception();
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            System.out.println("無足夠數量票券");
            return null;
        }

        return returnedList;
    }

    public Set<String> getLockedSeatsBySession(int sessionNo) {
        return seatsRedisDAO.setFindAllValues("Session" + sessionNo);
    }

    public boolean getSeasBySession(int sessionNo) {
        List<Seats> list = seatsRepository.findBySessionNo(sessionNo);
        if (!list.isEmpty()) {
            return true;
        }
        return false;
    }

    public List<Integer> getSeatsNumbers(int sessionNo, int blockNo) {
        List<Integer> list = new ArrayList<Integer>();
        list.add(seatsRepository.getToSellNumber(sessionNo, blockNo));
        list.add(seatsRepository.getSoldNumber(sessionNo, blockNo));
        return list;
    }


    public boolean copySessionSeats(int copiedSessionNo, int sessionNo, int activityNo) {
        // 直接取得場次座位表，並修改 sessionNo 後回傳到資料庫
        List<Seats> sessionSeats = seatsRepository.findBySessionNo(copiedSessionNo);

        for (Seats seat : sessionSeats) {

            Seats newSeat = new Seats();
            newSeat.setSessionNo(sessionNo);
            newSeat.setBlockNo(seat.getBlockNo());
            newSeat.setBlockName(seat.getBlockName());
            newSeat.setX(seat.getX());
            newSeat.setY(seat.getY());
            newSeat.setRealX(seat.getRealX());
            newSeat.setRealY(seat.getRealY());

            if (seat.getSeatStatus() == (-1))
                newSeat.setSeatStatus(-1);
            if (seat.getSeatStatus() == 0)
                newSeat.setSeatStatus(0);
            if (seat.getSeatStatus() == 1)
                newSeat.setSeatStatus(1);

            seatsRepository.save(newSeat);
        }

        return true;
    }

    public boolean copyModelSeats(int modelNo, int sessionNo, int activityNo) {
        // 取得欲複製模板的相關資訊
        PlaceToBlockModelDTO modelInfo = blockModelService.findByModelno(modelNo);
        List<BlockModel> modelBlockInfo = modelInfo.getBlockModels();
        // 取得節目的區域票價資訊
        List<BlockPrice> actBlockInfo = blockPriceService.getBlockPriceByActivityNo(activityNo);

        // 若欲複製模板的區域數量不等於節目區域數量，回傳 false
        if (modelBlockInfo.size() != actBlockInfo.size()) {
            return false;
        }

        // 確認節目複製當前的區域皆數對號入座 (blockType = 1)
        for (BlockPrice blockPrice : actBlockInfo) {
            if (blockPrice.getBlockType() != 1) {
                return false;
            }
        }

        // 新增一個 ArrayList 預備回傳到資料庫
        List<Seats> returendList = new ArrayList<Seats>();

        System.out.println("modelBlockInfo.size()=" + modelBlockInfo.size());

        // 依序取得模板區域
        for (int blockCount = 0; blockCount < modelBlockInfo.size(); blockCount++) {

            // 取得第 n 個模板區域的模板座位
            List<SeatsModel> seatsModels = modelBlockInfo.get(blockCount).getSeatsModels();

            // 取得節目區域的編號及名稱
            int toBlockNo = actBlockInfo.get(blockCount).getBlockNo();
            String toBlockName = actBlockInfo.get(blockCount).getBlockName();

            System.out.println("toBlockNo=" + toBlockNo);
            System.out.println("toBlockName=" + toBlockName);

            for (SeatsModel sm : seatsModels) {
                Seats seat = new Seats();
                seat.setSessionNo(sessionNo);
                seat.setBlockNo(toBlockNo);
                seat.setBlockName(toBlockName);
                seat.setX(sm.getX());
                seat.setY(sm.getY());
                seat.setRealX(sm.getRealx());
                seat.setRealY(sm.getRealy());
                seat.setSeatStatus(sm.getSeatStatus());

                returendList.add(seat);
            }
        }


        // 將異動完成的座位存起來
        returendList.stream()
                .sorted(Comparator.comparing(Seats::getX).thenComparing(Seats::getY))
                .forEach(seatsRepository::save);
//        for (Seats seat : returendList) {
//            System.out.println(seat);
//            seatsRepository.save(seat);
//        }

        return true;

        // 取得所有模板區域
        // List<BlockModelVO> modelBlocks = blockModelDAOImpl.getAll();

        // 取得所有模板的座位
        // List<SeatsModelVO> modelSeats = seatsModelDAOImpl.getAll();

        // 排除非所需模板的區域
        // List<BlockModelVO> targetmodelBlocks = new ArrayList<BlockModelVO>();

        // for (BlockModelVO modelblock : modelBlocks) {
        //     if (modelblock.getModelno() == modelNo) {
        //         targetmodelBlocks.add(modelblock);
        //     }
        // }

        // List<Integer> blockArr = new ArrayList<Integer>();
        // for (int i = 0; i < targetmodelBlocks.size(); i++) {
        //     blockArr.add(targetmodelBlocks.get(i).getBlockno());
        // }

        // 排除非所需模板區域的座位
        // List<SeatsModelVO> targetModelSeats = new ArrayList<SeatsModelVO>();

        // for (SeatsModelVO modelSeat : modelSeats) {
        //     if (blockArr.contains(modelSeat.getBlockno())) {
        //         targetModelSeats.add(modelSeat);
        //     }
        // }

        // 取得節目區域票價詳情
        // List<BlockPrice> actBlocks = blockPriceService.getBlockPriceByActivityNo(activityNo);
        // System.out.println("actBlocks=" + actBlocks);

        // 只要模板的區塊與節目的區塊數量相等就可以直接複製
        // int totalModelBlocks = targetmodelBlocks.size();
        // int totalActBlocks = actBlocks.size();

        // if (totalModelBlocks != totalActBlocks) {
        //     return false;
        // }

        // 確認兩個區塊編號的差異並修改模板中的區塊編號(從 1 開始)
        // int firstModelBlockNo = blockArr.get(0);
        // int firstActBlockNo = actBlocks.get(0).getBlockNo();
        // int countDiff = firstActBlockNo - firstModelBlockNo;

        // 新增一個 ArrayList 預備回傳到資料庫
        // List<Seats> returendList = new ArrayList<Seats>();

        // for (SeatsModelVO modelSeat : targetModelSeats) {

        //     int blockNo = modelSeat.getBlockno() + countDiff; //11 + 99993 = 100004
        //     System.out.println("blockNo= " + blockNo);
        //     String blockName = null;

        //     for(BlockPrice blockPrice: actBlocks){
        //         if(blockPrice.getBlockNo() == blockNo){
        //             blockName = blockPrice.getBlockName();
        //         }
        //     }
        //     String blockName = actBlocks.get(blockNo - (countDiff + 1)).getBlockName(); //100004-(99993+1) = 10
        //     System.out.println("blockName= " + blockName);

        //     Seats seat = new Seats();
        //     seat.setSessionNo(sessionNo);
        //     seat.setBlockNo(blockNo);
        //     seat.setBlockName(blockName);
        //     seat.setX(modelSeat.getX());
        //     seat.setY(modelSeat.getY());
        //     seat.setRealX(modelSeat.getRealx());
        //     seat.setRealY(modelSeat.getRealy());
        //     seat.setSeatStatus(modelSeat.getSeatStatus());

        //     returendList.add(seat);
        // }

        // 將異動完成的座位存起來
        // for (Seats seat : returendList) {
        //     System.out.println(seat);
        //     seatsRepository.save(seat);
        // }

        // return true;
    }

    public boolean deleteSeats(Integer sessionNo, Integer blockNo) {
        if (seatsRepository.deleteBySessionNoAndBlockNo(sessionNo, blockNo) > 0) {
            return true;
        }
        return false;
    }

    public Optional<Seats> findById(Integer seatNo) {
        return seatsRepository.findById(seatNo);
    }

    public Map<String, Integer> getSessionInfo(Integer sessionNo) {
        Map<String, Integer> returnedMap = new HashMap<String, Integer>();
        returnedMap.put("maxSeatsQty", seatsRepository.findSeatQtyBySessionNo(sessionNo));
        returnedMap.put("maxStandingQty", seatsRepository.findStandingQtyBySessionNo(sessionNo));
        returnedMap.put("seatsQty", seatsRepository.findSoldSeatQtyBySessionNo(sessionNo));
        returnedMap.put("standingQty", seatsRepository.findSoldStandingQtyBySessionNo(sessionNo));
        return returnedMap;
    }

    //    @Scheduled(cron = "0 0 0 * * *")
    @Scheduled(fixedRate = 300000)
    public void saveSeatsSets() {
        List<Session> sessions = sessionRepository.findAll();
        for (Session session : sessions) {

            List<Seats> seats = seatsRepository.findOrderedSeatsBySessionNo(session.getSessionNo());
            redisTemplate.delete("Session" + session.getSessionNo() + ":Set:2");
            redisTemplate.delete("Session" + session.getSessionNo() + ":Set:3");
            redisTemplate.delete("Session" + session.getSessionNo() + ":Set:4");

            // 取得兩個座位的組合
            for (int i = 0; i < seats.size() - 1; i++) {
                if (seats.get(i).getX() != seats.get(i + 1).getX()) {
                    continue;
                }

                List<Integer> toSaveSets = new ArrayList<Integer>();
                toSaveSets.add(seats.get(i).getSeatNo());
                toSaveSets.add(seats.get(i + 1).getSeatNo());
                seatsRedisDAO.setAddKV("Session" + session.getSessionNo() + ":Set:2", toSaveSets.toString());
            }

            // 取得三個座位的組合
            for (int i = 0; i < seats.size() - 2; i++) {
                if (seats.get(i).getX() != seats.get(i + 1).getX() ||
                        seats.get(i + 1).getX() != seats.get(i + 2).getX() ||
                        seats.get(i).getX() != seats.get(i + 2).getX()) {
                    continue;
                }

                List<Integer> toSaveSets = new ArrayList<Integer>();
                toSaveSets.add(seats.get(i).getSeatNo());
                toSaveSets.add(seats.get(i + 1).getSeatNo());
                toSaveSets.add(seats.get(i + 2).getSeatNo());
                seatsRedisDAO.setAddKV("Session" + session.getSessionNo() + ":Set:3", toSaveSets.toString());
            }

            // 取得四個座位的組合
            for (int i = 0; i < seats.size() - 3; i++) {
                if (seats.get(i).getX() != seats.get(i + 1).getX() ||
                        seats.get(i).getX() != seats.get(i + 2).getX() ||
                        seats.get(i).getX() != seats.get(i + 3).getX() ||
                        seats.get(i + 1).getX() != seats.get(i + 2).getX() ||
                        seats.get(i + 1).getX() != seats.get(i + 3).getX() ||
                        seats.get(i + 2).getX() != seats.get(i + 3).getX()) {
                    continue;
                }

                List<Integer> toSaveSets = new ArrayList<Integer>();
                toSaveSets.add(seats.get(i).getSeatNo());
                toSaveSets.add(seats.get(i + 1).getSeatNo());
                toSaveSets.add(seats.get(i + 2).getSeatNo());
                toSaveSets.add(seats.get(i + 3).getSeatNo());
                seatsRedisDAO.setAddKV("Session" + session.getSessionNo() + ":Set:4", toSaveSets.toString());
            }
        }
    }

    // 當使用者進到選頁面時，將顯示每個區域的剩餘可售票券數
    public Map<Integer, Integer> getToSellTQty(Integer activityNo, Integer sessionNo) {
        List<BlockPrice> blockList = blockPriceService.getBlockPriceByActivityNo(activityNo);
        Map<Integer, Integer> returnedMap = new HashMap<Integer, Integer>();
        for (BlockPrice block : blockList) {
            int toSellTQty = seatsRepository.getToSellingNumber(sessionNo, block.getBlockNo());
            returnedMap.put(block.getBlockNo(), toSellTQty);
        }
        return returnedMap;
    }

}
