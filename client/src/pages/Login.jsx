import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./css/Login.css"; // bạn đã có index.css

export default function LoginPage() {
  const [show, setShow] = useState(true);
  const Home = useNavigate();

  return (
    <div className="login-container">
      <div className="login-bg"></div>

      <div className="login-main">
        <div className="login-welcome">
          <h1 className="login-title"> ParkingSmart</h1>
          <p className="login-text">
            Chào mừng bạn trở lại,
            <br />
            hãy đăng nhập để có thể tiếp tục sử dụng dịch vụ của chúng tôi.
          </p>
        </div>

        <div className="login-form-wrapper">
          <div className="login-form-box">
            <div className="login-form-header">
              <h3 className="login-form-title">Đăng nhập tài khoản</h3>
              <p className="login-form-subtitle">
                Bạn chưa có tài khoản?{" "}
                <a href="/Register" className="login-link">
                  Đăng ký
                </a>
              </p>
            </div>

            <form className="login-form-body">
              <input type="email" placeholder="Tên đăng nhập / Email" className="login-input" />

              <div className="login-password-box">
                <input
                  placeholder="Mật khẩu"
                  type={show ? "password" : "text"}
                  className="login-input"
                />
                <button
                  type="button"
                  className="login-toggle-btn"
                  onClick={() => setShow(!show)}
                >
                  {show ? "👁️" : "🙈"}
                </button>
              </div>

              <div className="login-forgot">
                <a href="/ForgotPassword" className="login-link">
                  Quên mật khẩu?
                </a>
              </div>

              <button type="submit" className="login-btn-primary">
                Đăng nhập
              </button>

              <div className="login-divider">
                <span className="login-line"></span>
                <span className="login-or">Hoặc</span>
                <span className="login-line"></span>
              </div>

              <div className="login-social">
                <button className="login-btn-social login-google">
                  Google
                </button>
              </div>
            </form>
          </div>
        </div>
        <button className="login-btn-back" onClick={() => Home("/")}>
          Quay lại trang chủ
        </button>
      </div>
    </div>
  );
}
