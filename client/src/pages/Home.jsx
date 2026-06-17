import "./css/Home.css";
import Header from "../components/NonAuthHeader";
import NonAuthFooter from "../components/NonAuthFooter";

function Home() {
  return (
    <div className="home-page">
      {/* Header Section */}
      <Header />
      {/* Stats Banner Section */}
      <section className="home-stats-section">
        <div className="home-stats-container">
          <div className="home-stat-box">
            <span className="home-stat-label">Giờ hoạt động</span>
            <span className="home-stat-number">4h00 - 23h00</span>
          </div>
          <div className="home-stat-box">
            <span className="home-stat-label">Vị trí trống</span>
            <span className="home-stat-number">
              <span style={{ "font-size": "1.6rem" }}>16</span>/100
            </span>
          </div>
          <div className="home-stat-box">
            <span className="home-stat-label">Hỗ trợ</span>
            <span className="home-stat-number">24/7</span>
          </div>
          <div className="home-stat-box">
            <span className="home-stat-label">Trang thái</span>
            <span className="home-stat-number">Đang hoạt động</span>
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section className="home-pricing-section" id="pricing">
        <h2>Bảng giá gửi xe</h2>
        <p className="home-subtitle">
          Phí giờ cao điểm sẽ{" "}
          <span style={{ color: "red", fontWeight: "bold" }}> +20% </span>(16h00
          - 20h00)
        </p>

        <table className="home-pricing-table">
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
                <span className="home-vehicle-icon">🏍️</span> <span className="home-vehicle-name">Xe máy</span>
              </td>
              <td>5.000 <span className="home-unit">VNĐ</span></td>
              <td>10.000 <span className="home-unit">VNĐ</span></td>
              <td>8.000 <span className="home-unit">VNĐ</span></td>
            </tr>
            <tr>
              <td style={{ "text-align": "left" }}>
                <span className="home-vehicle-icon">🚗</span> <span className="home-vehicle-name">Ô tô</span>
              </td>
              <td>20.000 <span className="home-unit">VNĐ</span></td>
              <td>50.000 <span className="home-unit">VNĐ</span></td>
              <td>14.000 <span className="home-unit">VNĐ</span></td>
            </tr>
          </tbody>
        </table>

        {/* Monthly Pass Section */}
        <div className="home-monthly-pass" id="monthly">
          <div className="home-monthly-pass-banner">
            <div className="home-monthly-pass-badge">Hot</div>

            <div className="home-monthly-pass-container">
              {/* Left - Info */}
              <div className="home-monthly-pass-info">
                <h2>VÉ THÁNG CHO PHƯƠNG TIỆN</h2>
                <p className="home-pass-description">
                  Có thể ra vào bất cứ khi nào, đảm bảo chỗ đỗ xe, 
                  <br/>phù hợp cho khách hàng thường xuyên.
                </p>
                <ul className="home-benefits-list">
                  <li>
                    <span className="home-benefit-icon">✅</span>
                    <span>Ưu tiên vào/ra</span>
                  </li>
                  <li>
                    <span className="home-benefit-icon">✅</span>
                    <span>Tiết kiệm đến 20% so với giá theo giờ</span>
                  </li>
                  <li>
                    <span className="home-benefit-icon">✅</span>
                    <span>
                      Quản lý nhiều phương tiện trong cùng một tài khoản
                    </span>
                  </li>
                </ul>
                <button className="home-btn-register">Đăng ký ngay</button>
              </div>

              {/* Right - Card Image */}
              <div className="home-monthly-pass-card">
                <img
                  src="https://images.unsplash.com/photo-1607860108855-64acf2078ed9?w=400&h=250&fit=crop"
                  alt="Smart parking card"
                  className="home-pass-image"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Enterprise Contact Section */}
        <div className="home-enterprise-section" id="contact">
          <h2>Liên hệ doanh nghiệp</h2>
          <p className="home-subtitle">
            Liên hệ với chúng tôi để biết thêm về các giải pháp đậu xe dành cho
            bạn
          </p>

          <div className="home-contact-info">
            <div className="home-contact-card">
              <span className="home-contact-icon">📞</span>
              <h4>Số điện thoại</h4>
              <p>+84 123 456 789</p>
              <p className="home-small">Hàng ngày 8h00 - 18h00</p>
            </div>
            <div className="home-contact-card">
              <span className="home-contact-icon">✉️</span>
              <h4>Email</h4>
              <p>ParkingSmart@gmail.com</p>
              <p className="home-small">Thời gian liên hệ trong 24 giờ</p>
            </div>
            <div className="home-contact-card">
              <span className="home-contact-icon">📍</span>
              <h4>Địa chỉ</h4>
              <p>123 đường ABC, Quận 1</p>
              <p className="home-small">Tp.Hồ Chí Minh - Việt Nam</p>
            </div>
          </div>
        </div>
      </section>
      <NonAuthFooter />
    </div>
  );
}

export default Home;
