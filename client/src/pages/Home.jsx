import "./css/Home.css";

function Home() {
  return (
    <div className="parking-page">
      {/* Header Section */}
      <header className="header">
        <nav className="navbar">
          <div className="logo">
            <span className="logo-icon">
              <img src="/logo.png" alt="Logo" />
            </span>
            <span className="logo-text">ParkingSmart</span>
          </div>
          <ul className="nav-links">
            <li>
              <a href="#Home" className="active">
                Trang chủ
              </a>
            </li>
            <li>
              <a href="#Reservation">Đặt chỗ</a>
            </li>
            <li>
              <a href="#News">Tin tức</a>
            </li>
            <li>
              <a href="#Support">Hỗ trợ</a>
            </li>
          </ul>
          <button className="btn-signin">Đăng nhập</button>
        </nav>

        <div className="hero">
          <div className="hero-content">
            <div className="hero-text">
              <h1>
                Giải pháp thông minh
                <br />
                cho mọi nhu cầu đỗ xe
              </h1>
              <p>
                Trải nghiệm quản lý đậu xe dễ dàng với tính năng theo dõi vị trí
                đậu xe theo thời gian thực, thanh toán tự động và các tiện ích
                an toàn được thiết kế dành cho người lái xe hiện đại.
              </p>
              <div className="hero-buttons">
                <button className="btn-primary">Learn More</button>
              </div>
            </div>
            <div className="hero-image">
              <img
                src="https://images.unsplash.com/photo-1506521781263-d8422e82f27a?w=600&h=400&fit=crop"
                alt="Modern parking garage"
              />
            </div>
          </div>
        </div>
      </header>

      {/* Stats Banner Section */}
      <section className="stats-section">
        <div className="stats-container">
          <div className="stat-box">
            <span className="stat-label">Giờ hoạt động</span>
            <span className="stat-number">4h00 - 23h00</span>
          </div>
          <div className="stat-box">
            <span className="stat-label">Vị trí trống</span>
            <span className="stat-number">
              <span style={{ "font-size": "1.6rem" }}>16</span>/100
            </span>
          </div>
          <div className="stat-box">
            <span className="stat-label">Hỗ trợ</span>
            <span className="stat-number">24/7</span>
          </div>
          <div className="stat-box">
            <span className="stat-label">Trang thái</span>
            <span className="stat-number">Đang hoạt động</span>
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section className="pricing-section" id="pricing">
        <h2>Bảng giá gửi xe</h2>
        <p className="subtitle">
          Phí giờ cao điểm sẽ{" "}
          <span style={{ color: "red", fontWeight: "bold" }}> +20% </span>(16h00
          - 20h00)
        </p>

        <table className="pricing-table">
          <thead>
            <tr>
              <th>Loại phương tiện</th>
              <th>Theo giờ</th>
              <th>Theo ngày</th>
              <th>Qua đêm</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style={{ "text-align": "left" }}>
                <span className="vehicle-icon">🏍️</span> <span className="vehicle-name">Xe máy</span>
              </td>
              <td>5.000 <span className="unit">VNĐ</span></td>
              <td>10.000 <span className="unit">VNĐ</span></td>
              <td>8.000 <span className="unit">VNĐ</span></td>
            </tr>
            <tr>
              <td style={{ "text-align": "left" }}>
                <span className="vehicle-icon">🚗</span> <span className="vehicle-name">Ô tô</span>
              </td>
              <td>20.000 <span className="unit">VNĐ</span></td>
              <td>50.000 <span className="unit">VNĐ</span></td>
              <td>14.000 <span className="unit">VNĐ</span></td>
            </tr>
          </tbody>
        </table>

        {/* Monthly Pass Section */}
        <div className="monthly-pass" id="monthly">
          <div className="monthly-pass-banner">
            <div className="monthly-pass-badge">Hot</div>

            <div className="monthly-pass-container">
              {/* Left - Info */}
              <div className="monthly-pass-info">
                <h2>VÉ THÁNG CHO PHƯƠNG TIỆN</h2>
                <p className="pass-description">
                  Có thể ra vào bất cứ khi nào, đảm bảo chỗ đỗ xe, 
                  <br/>phù hợp cho khách hàng thường xuyên.
                </p>
                <ul className="benefits-list">
                  <li>
                    <span className="benefit-icon">✅</span>
                    <span>Ưu tiên vào/ra</span>
                  </li>
                  <li>
                    <span className="benefit-icon">✅</span>
                    <span>Tiết kiệm đến 20% so với giá theo giờ</span>
                  </li>
                  <li>
                    <span className="benefit-icon">✅</span>
                    <span>
                      Quản lý nhiều phương tiện trong cùng một tài khoản
                    </span>
                  </li>
                </ul>
                <button className="btn-register">Đăng ký ngay</button>
              </div>

              {/* Right - Card Image */}
              <div className="monthly-pass-card">
                <img
                  src="https://images.unsplash.com/photo-1607860108855-64acf2078ed9?w=400&h=250&fit=crop"
                  alt="Smart parking card"
                  className="pass-image"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Enterprise Contact Section */}
        <div className="enterprise-section" id="contact">
          <h2>Liên hệ doanh nghiệp</h2>
          <p className="subtitle">
            Liên hệ với chúng tôi để biết thêm về các giải pháp đậu xe dành cho
            bạn
          </p>

          <div className="contact-info">
            <div className="contact-card">
              <span className="contact-icon">📞</span>
              <h4>Số điện thoại</h4>
              <p>+84 123 456 789</p>
              <p className="small">Hàng ngày 8h00 - 18h00</p>
            </div>
            <div className="contact-card">
              <span className="contact-icon">✉️</span>
              <h4>Email</h4>
              <p>ParkingSmart@gmail.com</p>
              <p className="small">Thời gian liên hệ trong 24 giờ</p>
            </div>
            <div className="contact-card">
              <span className="contact-icon">📍</span>
              <h4>Địa chỉ</h4>
              <p>123 đường ABC, Quận 1</p>
              <p className="small">Tp.Hồ Chí Minh - Việt Nam</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer">
        <p>&copy; 2026 ParkEase Inc</p>
        <div className="footer-links">
          <a href="#privacy">Chính sách bảo mật</a>
          <a href="#terms">Điều khoản dịch vụ</a>
        </div>
      </footer>
    </div>
  );
}

export default Home;
