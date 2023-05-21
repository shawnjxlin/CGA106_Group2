<!-- 選擇座位的JS -->
// WebSocket 用戶相關變數與函式 (connect/ sendMessage/ disconnect)
let webSocket;

function connect() {
    let MyPoint = "/SelectSeatsTogether/Sync";
    let path = window.location.pathname;
    let webCtx = path.substring(0, path.indexOf('/', 1));
    let endPointURL = "ws://" + window.location.host + webCtx + MyPoint;

    // 建立 WebSocket 連線
    webSocket = new WebSocket(endPointURL);

    // WebSocket 啟動動作
    webSocket.onopen = function (event) {
    };

    // WebSocket 接收推播動作
    webSocket.onmessage = function (event) {
        let jsonObj = JSON.parse(event.data);
        let seat = document.getElementById(`${jsonObj.seatNo}`);

        if (jsonObj.seatStatus == 3) {
            seat.classList.remove("toSell");
            seat.classList.add("beLocked");
        } else {
            seat.classList.remove("beLocked");
            seat.classList.add("toSell");
        }
    };

    // WebSocket 結束使用動作
    webSocket.onclose = function (event) {
    };
}

// WebSocket Client 向 Server 端發動訊息的動作
function sendMessage(seat) {
    let jsonObj;
    if (seat.classList.contains("selected")) {
        jsonObj = {
            "sessionNo": sessionStorage.getItem("sessionNo"),
            "seatNo": seat.id,
            "seatStatus": 3
        };
        fetch("seats/updateSeatStatus" + `?seatStatus=${jsonObj.seatStatus}&seatNo=${jsonObj.seatNo}&sessionNo=${jsonObj.sessionNo}`);
    } else {
        jsonObj = {
            "sessionNo": sessionStorage.getItem("sessionNo"),
            "seatNo": seat.id,
            "seatStatus": 1
        };
        fetch("seats/updateSeatStatus" + `?seatStatus=${jsonObj.seatStatus}&seatNo=${jsonObj.seatNo}&sessionNo=${jsonObj.sessionNo}`);
    }
    webSocket.send(JSON.stringify(jsonObj));
}

function disconnect() {
    webSocket.close();
}

// 選位相關變數與函式
let seats;
let t_seats;
let t_Row;
let t_Column;
let count;
let lockedSeats = [];
let layout = document.getElementById("layout");

let seatsmgt = {

    init: () => {
        layout.innerHTML = "";

        // 初始化票券數量 (最小為 1 張)
        $('#quantity').val("1");

        // 初始化已選位數量
        count = 0;

        $.ajax({ // 取得場次所選擇區域的座位表 (MySQL)
            async: false,
            type: 'GET',
            url: '/seats/BySessionAndBlock' + `?sessionNo=${sessionStorage.getItem("sessionNo")}&blockNo=${sessionStorage.getItem("blockNo")}`,
            dataType: "json",
            success: function (jsondata) {
                seats = jsondata;
            }
        });

        t_seats = seats.length;
        t_Row = seats[seats.length - 1].x;
        t_Column = seats[seats.length - 1].y;

        $('#blockName').text(seats[0].blockName);

        // 購買數量 < 選擇數量時，要求更新購買數量
        $('#quantity').change(function () {
                if ($('#quantity').val() < count) {
                    Swal.fire({
                        icon: 'error',
                        title: '選擇座位數量已超出選購數量，請再次確認'
                    });

                    return;
                }
            }
        );

        // 座位每 x 個換一排
        layout.setAttribute("style", `grid-template-columns: repeat(${(t_Column + 1) * 2}, 1fr)`);

        // GENERATE SEATS
        for (let i = 0; i <= t_Row; i++) {
            for (let j = 0; j <= t_Column; j++) {
                let seat = document.createElement("div");

                // ID 為座位編號(seatno)
                seat.id = seats[i * (t_Column + 1) + j].seatNo;

                seat.innerHTML = `
        				第 <span>${seats[i * (t_Column + 1) + j].realX}</span> 排<br>
        				第 <span>${seats[i * (t_Column + 1) + j].realY}</span> 位
        				`;

                seat.className = "seat";
                seat.onclick = () => seatsmgt.toggle(seat);

                let seatStatus = seats[i * (t_Column + 1) + j].seatStatus;

                if (!lockedSeats.includes(String(seat.id))) {
                    switch (seatStatus) {
                        case (-1):
                            seat.classList.add("banned");
                            break;
                        case (0):
                            seat.classList.add("taken");
                            break;
                        case (1):
                            seat.classList.add("toSell");
                            break;
                        case (2):
                            seat.classList.add("taken");
                            break;
                    }
                } else {
                    seat.classList.add("beLocked");
                }

                layout.appendChild(seat);

                // 此行若不加，則座位表會跑版
                layout.appendChild(document.createElement("br"));
            }
        }
    },

    toggle: seat => {

        if ($('#quantity').val() < count) {
            --count;
            seat.classList.remove("selected");
            seat.classList.add("toSell");
            sendMessage(seat);
        }

        if (seat.classList.contains("toSell")) {
            if ($('#quantity').val() > count) {
                ++count;
                seat.classList.remove("toSell");
                seat.classList.add("selected");
                sendMessage(seat);
            }
            return;
        }

        if (seat.classList.contains("selected")) {
            if ($('#quantity').val() >= count) {
                --count;
                seat.classList.remove("selected");
                seat.classList.add("toSell");
                sendMessage(seat);
            }
            return;
        }
    },

    save: () => {
        let seats = document.querySelectorAll("#layout div.selected");
        let seatNos = [];

        for (let s of seats) {
            seatNos.push(s.id);
        }

        if (seats.length === 0 || seats.length < $('#quantity').val() || seats.length > $('#quantity').val()) {
            Swal.fire({
                icon: 'error',
                title: '請確認是否已選擇對應數量的座位'
            });

            return;
        }

        sessionStorage.setItem("toBuySeats", JSON.stringify(seatNos));
        window.location.href = "/front-activity-checkout.html";
    },

    cancel: () => {
        // 重設選擇票券數量
        count = 0;
        let seats = document.querySelectorAll("#layout div.selected");
        for (let s of seats) {
            s.classList.remove("selected");
            s.classList.add("toSell");
            sendMessage(s);
        }
    }
};

let aType;
let toSellTqy;

// 頁面載入動作
$(document).ready(function () {
    localStorage.removeItem('currentUrl');

    let activityNo = sessionStorage.getItem("activityNo");
    if (activityNo === null || activityNo === "") {
        location.href = `front-activity-event-explore_events.html`;
    }

    $.ajax({
        async: false,
        type: 'GET',
        url: "/member/getMemberInfo",
        dataType: "json",
        success: function (jsondata) {
            memberNo = jsondata['memberno'];
            if (memberNo === null) {
                localStorage.setItem('currentUrl', window.location.href);
                window.location.href = "/front-users-mem-sign-in.html";
            }
        }
    })

    // WebSocket 連線
    connect();

    // 清空下拉選單(selectSession)/ 按鈕(selectBlocks)/ 節目名稱及表演時間
    document.getElementById('selectSession').innerHTML = "";
    document.getElementById('actName').innerText = "";
    document.getElementById('showTime').innerText = "";

    // 取得場次資訊並加入下拉選單
    fetch("Session/ActSessions" + `?actNo=${sessionStorage.getItem("activityNo")}`)
        .then(resp => resp.json())
        .then(jsondata => {
            for (let j of jsondata) {
                if (j.sessionNo == sessionStorage.getItem("sessionNo")) {
                    document.getElementById('selectSession').innerHTML += `
                            <option value="${j.sessionNo}" selected>${j.sessionsTime} ${j.activity.aname}</option>
                            `;

                    document.getElementById('actName').innerText = `${j.activity.aname}`;

                    document.getElementById('showTime').innerText = `${j.sessionsTime}`;

                } else {
                    document.getElementById('selectSession').innerHTML += `
                            <option value="${j.sessionNo}">${j.sessionsTime} ${j.activity.aname}</option>
                            `;
                }
            }
        })

    // 取得節目主視覺、座位圖並呈現於 HTML
    $.ajax({
        async: false,
        type: 'GET',
        url: "/Activity/findByActNo" + `?actNo=${sessionStorage.getItem("activityNo")}`,
        dataType: "json",
        success: function (jsondata) {
            document.getElementById('seatsImg').src = `data:image/png;base64,${jsondata.aseatsImg}`;

            let showpic = jsondata.aimgt;

            for (let i = 0; i < showpic.length; i++) {
                if (showpic[i].aimgMain == 1) {
                    document.getElementById('actImg').src = `data:image/png;base64,${showpic[i].aimg}`;
                }
            }
            aType = jsondata.wetherSeat;
        }
    });

    if (aType === 1) {
        $.ajax({
            async: false,
            type: 'GET',
            url: "/seats/getBlockToSellQty" + `?activityNo=${sessionStorage.getItem("activityNo")}&sessionNo=${sessionStorage.getItem("sessionNo")}`,
            dataType: "json",
            success: function (jsondata) {
                toSellTqy = jsondata;
            }
        });
    } else {
        $.ajax({
            async: false,
            type: 'GET',
            url: "/Session/getBlockToSellQty" + `?activityNo=${sessionStorage.getItem("activityNo")}&sessionNo=${sessionStorage.getItem("sessionNo")}`,
            dataType: "json",
            success: function (jsondata) {
                toSellTqy = jsondata;
            }
        });
    }

    // 預設為系統選位
    selectBySystem();

    // 每秒得到最新的 Redis 座位狀態
    setInterval('getLastestLockedSeats();', 1000);
})

// 選擇場次後重新整理 HTML
function selectSession() {
    sessionStorage.setItem("sessionNo", `${$("#selectSession").val()}`);
    window.location.reload();
}

// 點選系統選位按鈕時，將按鈕的 data-bs-target 改為 "#orgSettings_system"
function selectBySystem() {
    document.getElementById('selectBlocks').innerHTML = "";

    fetch("BlockPrice/findAll")
        .then(resp => resp.json())
        .then(jsondata => {
                for (let j of jsondata) {
                    if (j.activityNo == sessionStorage.getItem("activityNo")) {
                        document.getElementById('selectBlocks').innerHTML += `
                                <button name="selectOneBlock" value="${j.blockNo}"
                                    class="main-btn h_40 w-100 mt-5" type="button"
                                    data-bs-toggle="modal" data-bs-target="#orgSettings_system"
                                    style="margin: 5px!important; background-color: #efedf5;
											     border: 1px solid #102743; font-size: 1rem; color: black;"
                                    onMouseDown="setKV(this.value)" onclick="setBlockNoAndName()">${j.blockName}
                                    <span style="color: red">&emsp;剩餘 ${toSellTqy[j.blockNo]} 張</span>
                                </button>`;
                    }
                }

                if (document.getElementById('selectBlocks').innerHTML === "") {
                    document.getElementById('selectBlocks').innerHTML += `
                                <button name="selectOneBlock" value=""
                                    class="main-btn h_40 w-100 mt-5" type="button"
                                    data-bs-toggle="modal" data-bs-target="#orgSettings_system"
                                    style="margin: 5px!important; background-color: #efedf5;
											     border: 1px solid #102743; font-size: 1rem; color: black;">
							        立即購票
                                </button>`;
                }
            }
        )
}

// 點選自行選位按鈕時，將按鈕的 data-bs-target 改為 "#orgSettings"，但如果對應區域沒有座位時，依然保持 "#orgSettings_system"
function selectByMember() {
    document.getElementById('selectBlocks').innerHTML = "";

    fetch("BlockPrice/findAll")
        .then(resp => resp.json())
        .then(jsondata => {
                for (let j of jsondata) {
                    if (j.activityNo == sessionStorage.getItem("activityNo")) {
                        if (j.blockType === 1) {
                            document.getElementById('selectBlocks').innerHTML += `
                                <button name="selectOneBlock" value="${j.blockNo}"
                                    class="main-btn h_40 w-100 mt-5" type="button"
                                    data-bs-toggle="modal" data-bs-target="#orgSettings"
                                    style="margin: 5px!important; background-color: #efedf5;
											     border: 1px solid #102743; font-size: 1rem; color: black;"
                                    onMouseDown="setKV(this.value)" onclick="seatsmgt.init()">${j.blockName}
                                    <span style="color: red">&emsp;剩餘 ${toSellTqy[j.blockNo]} 張</span>
                                </button>`;
                        } else {
                            document.getElementById('selectBlocks').innerHTML += `
                                <button name="selectOneBlock" value="${j.blockNo}"
                                        class="main-btn h_40 w-100 mt-5" type="button"
                                        data-bs-toggle="modal" data-bs-target="#orgSettings_system"
                                        style="margin: 5px!important; background-color: #efedf5;
											     border: 1px solid #102743; font-size: 1rem; color: black;"
                                        onMouseDown="setKV(this.value)">${j.blockName}
                                    <span style="color: red">&emsp;剩餘 ${toSellTqy[j.blockNo]} 張</span>
                                </button>`
                        }
                    }
                }

                if (document.getElementById('selectBlocks').innerHTML === "") {
                    document.getElementById('selectBlocks').innerHTML += `
                                <button name="selectOneBlock" value=""
                                    class="main-btn h_40 w-100 mt-5" type="button"
                                    data-bs-toggle="modal" data-bs-target="#orgSettings_system"
                                    style="margin: 5px!important; background-color: #efedf5;
											     border: 1px solid #102743; font-size: 1rem; color: black;">
							        立即購票
                                </button>`;
                }
            }
        )
}


// 零碎的 JS 函式

// 點選區塊按鈕後將區塊編號存入 SessionStorage
function setKV(blockNo) {
    sessionStorage.setItem("blockNo", blockNo);
}

// 系統選位的顯示視窗，應顯示對應區域名稱
function setBlockNoAndName() {
    clickChange();
    fetch("/BlockPrice/findById" + `?blockNo=${sessionStorage.getItem("blockNo")}`)
        .then(resp => resp.json())
        .then(jsondata => {
            document.getElementById('blockName_system').innerHTML = "";
            document.getElementById('blockName_system').innerHTML = `${jsondata.blockName}`;
        })
}

// 監聽浮動視窗點擊空白 => 清空 Redis 所存的鎖定座位
$('#orgSettings').on('hidden.bs.modal', function (e) {
    seatsmgt.cancel();
});

// 監聽浮動視窗點擊空白 => 歸零票券數量
$('#orgSettings_system').on('hidden.bs.modal', function (e) {
    $('#ticketDecr option[value="1"]').prop("selected", true);
});

// 取得最新的 Redis 座位狀態
function getLastestLockedSeats() {
    $.ajax({
        async: false,
        type: 'GET',
        url: '/seats/getLockedSeatsBySession' + `?sessionNo=${sessionStorage.getItem("sessionNo")}`,
        dataType: "json",
        success: function (jsondata) {
            lockedSeats = jsondata;
        }
    });
}

// 系統選位 => 購票
function TicketBySystem() {

    if (verify() === false) {
        return;
    }

    fetch("/Activity/findByActNo" + `?actNo=${sessionStorage.getItem("activityNo")}`)
        .then(resp => resp.json())
        .then(jsondata => {
            if (aType !== 1) { // 若節目完全沒有座位區分，將要購買的票券數量存在 SessionStorage

                $.ajax({
                    async: false,
                    type: 'GET',
                    url: "/Session/getBlockToSellQty" + `?activityNo=${sessionStorage.getItem("activityNo")}&sessionNo=${sessionStorage.getItem("sessionNo")}`,
                    dataType: "json",
                    success: function (jsondata) {
                        realToSellTqy = jsondata;
                    }
                });


                if ($('#ticketDecr').val() > realToSellTqy[sessionStorage.getItem("blockNo")] || Object.keys(realToSellTqy).length === 0) {
                    Swal.fire({
                        icon: 'error',
                        title: '無對應數量票券'
                    })

                    setTimeout(function () {
                        location.reload();
                    }, 3000);

                    return;
                }

                sessionStorage.setItem("toBuyTickets", $('#ticketDecr').val());
                window.location.href = "/front-activity-checkout.html";
            } else { // 若節目有座位區分，發送請求到後端取得座位陣列
                fetch("/seats/getTicketsBysystem" + `?ticketQTY=${$('#ticketDecr').val()}&blockNo=${sessionStorage.getItem("blockNo")}&sessionNo=${sessionStorage.getItem("sessionNo")}`)
                    .then(resp => resp.json())
                    .then(jsondata => {

                        if (jsondata.length === 0) {
                            $('#toBeVerified').val("");

                            Swal.fire({
                                icon: 'error',
                                title: '無對應數量票券'
                            })

                            return;
                        }

                        if (jsondata) {
                            let seatNos = [];
                            let count = 0;

                            for (let j of jsondata) {
                                seatNos.push(j);
                                // 包裝取得的座位陣列，並送給 WebSocket 伺服器推播
                                let seat = document.createElement("div");
                                seat.id = j;
                                seat.classList.add("selected");
                                sendMessage(seat);
                                ++count;
                            }

                            if(count === jsondata.length){
                                // 將要購買的座位編號以陣列的格式存在 SessionStorage
                                sessionStorage.setItem("toBuySeats", JSON.stringify(seatNos));
                                window.location.href = "/front-activity-checkout.html";
                                return;
                            }
                        }
                    }).catch(error => {
                        $('#toBeVerified').val("");

                        Swal.fire({
                            icon: 'error',
                            title: '無對應數量票券'
                        })

                        return;
                    })
            }
        })
}