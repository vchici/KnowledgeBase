/* ============================================================
   Harness 课程共享测验组件
   用法：在课程 HTML 中定义 <div class="quiz" data-quiz="..."></div>
        页面底部用 <script> 声明 QUIZ_DATA 并调用 renderQuizzes()
   反馈环：选错可重试（标红），选对立即锁定并展示解析——
   检索练习 + 即时反馈，两全。
   ============================================================ */

(function () {
  var state = { answered: 0, correctFirstTry: 0, total: 0 };

  function build(container, q, index) {
    container.classList.add("quiz");
    var qText = document.createElement("div");
    qText.className = "q-text";
    qText.textContent = "Q" + (index + 1) + " · " + q.q;
    container.appendChild(qText);

    var explain = document.createElement("div");
    explain.className = "explain";

    q.options.forEach(function (opt) {
      var btn = document.createElement("button");
      btn.type = "button";
      btn.className = "opt";
      btn.textContent = opt.t;
      var firstTry = true; // 本题是否尚未答错

      btn.addEventListener("click", function () {
        if (opt.ok) {
          btn.classList.add("correct");
          Array.prototype.forEach.call(
            container.querySelectorAll(".opt"),
            function (b) { b.disabled = true; }
          );
          explain.className = "explain show ok";
          explain.innerHTML = "✓ " + q.explain;
          state.answered++;
          if (firstTry) state.correctFirstTry++;
          if (state.answered === state.total) showSummary(container.ownerDocument);
        } else {
          firstTry = false;
          btn.classList.add("wrong");
          btn.disabled = true;
          explain.className = "explain show retry";
          explain.innerHTML = "✗ " + (opt.why || "再想想——回顾上文再选一次。");
        }
      });
      container.appendChild(btn);
    });

    container.appendChild(explain);
  }

  function showSummary(doc) {
    var s = doc.querySelector(".quiz-summary");
    if (!s) return;
    var msg;
    if (state.correctFirstTry === state.total) {
      msg = "全对（" + state.correctFirstTry + "/" + state.total + "）——检索一次命中，存储强度扎实。下一课见。";
    } else if (state.correctFirstTry >= state.total / 2) {
      msg = "答对 " + state.correctFirstTry + "/" + state.total + "。有价值的错误：它们标记了值得重读的段落，隔天回来重做一遍本测验。";
    } else {
      msg = "答对 " + state.correctFirstTry + "/" + state.total + "——很正常，概念刚建立。建议：回到正文「五大件」一节重读，明天再做一次本页测验（间隔重取）。";
    }
    s.textContent = msg;
    s.classList.add("show");
  }

  window.renderQuizzes = function (data) {
    var containers = document.querySelectorAll("[data-quiz]");
    state.total = containers.length;
    var i = 0;
    Array.prototype.forEach.call(containers, function (c) {
      var q = data[c.getAttribute("data-quiz")];
      if (q) build(c, q, i++);
    });
  };
})();
