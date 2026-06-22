
    create table ad_metrics (
        advertiser_id bigint not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        ip_address varchar(255),
        type enum ('CLICK','IMPRESSION') not null,
        primary key (id)
    ) engine=InnoDB;

    create table ad_requests (
        duration_months integer,
        created_at datetime(6),
        end_date datetime(6),
        id bigint not null auto_increment,
        start_date datetime(6),
        button_text varchar(50),
        ad_description TEXT,
        ad_image_url varchar(255),
        ad_title varchar(255),
        company_description TEXT,
        company_name varchar(255) not null,
        contact_name varchar(255) not null,
        email varchar(255) not null,
        logo_url varchar(255),
        message TEXT,
        phone varchar(255),
        redirect_url varchar(255),
        website_url varchar(255),
        placement enum ('CHECKOUT','PRODUCT_DETAIL','PRODUCT_LIST'),
        status enum ('APPROVED','EXPIRED','PENDING','REJECTED'),
        primary key (id)
    ) engine=InnoDB;

    create table addresses (
        is_default boolean default false not null,
        latitude float(53),
        longitude float(53),
        id bigint not null auto_increment,
        user_id bigint not null,
        receiver_phone varchar(50),
        title varchar(50) not null,
        apartment_office varchar(100),
        city varchar(100) not null,
        country varchar(100),
        department varchar(100),
        neighborhood varchar(100),
        receiver_name varchar(100),
        street varchar(200) not null,
        image_url varchar(500),
        reference varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table advertisers (
        active bit not null,
        ad_order integer,
        duration_months integer,
        clicks_count bigint,
        end_date datetime(6),
        id bigint not null auto_increment,
        start_date datetime(6),
        views_count bigint,
        button_text varchar(50),
        ad_description TEXT,
        ad_image_url varchar(255),
        ad_title varchar(255),
        company_description TEXT,
        contact_email varchar(255),
        contact_name varchar(255),
        logo_url varchar(255),
        name varchar(255) not null,
        phone varchar(255),
        redirect_url varchar(255),
        website_url varchar(255),
        placement enum ('CHECKOUT','PRODUCT_DETAIL','PRODUCT_LIST'),
        primary key (id)
    ) engine=InnoDB;

    create table app_settings (
        base_distance_km float(53),
        cost_per_km decimal(10,2),
        default_shipping_cost decimal(38,2),
        distance_shipping_enabled bit,
        free_shipping_threshold decimal(38,2),
        max_image_size_mb integer,
        max_pdf_size_mb integer,
        store_latitude float(53),
        store_longitude float(53),
        tax_enabled bit,
        tax_rate decimal(5,2),
        id bigint not null auto_increment,
        address varchar(500),
        footer_text varchar(500),
        meta_description varchar(500),
        meta_keywords varchar(500),
        bg_dark_url varchar(255),
        bg_light_url varchar(255),
        contact_email varchar(255),
        contact_phone varchar(255),
        copyright_text varchar(255),
        facebook_url varchar(255),
        favicon_url varchar(255),
        instagram_url varchar(255),
        logo_url varchar(255),
        meta_title varchar(255),
        store_name varchar(255) not null,
        tagline varchar(255),
        whatsapp_number varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table banner_metrics (
        banner_id bigint not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        ip_address varchar(255),
        type enum ('CLICK','IMPRESSION') not null,
        primary key (id)
    ) engine=InnoDB;

    create table banners (
        active bit not null,
        banner_order integer,
        clicks_count bigint,
        end_date datetime(6),
        id bigint not null auto_increment,
        start_date datetime(6),
        views_count bigint,
        button_text varchar(50),
        description TEXT,
        image_url varchar(255) not null,
        link_url varchar(255),
        title varchar(255),
        placement enum ('CHECKOUT','HOME_CAROUSEL','PRODUCT_DETAIL','SIDEBAR') not null,
        primary key (id)
    ) engine=InnoDB;

    create table brands (
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table categories (
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table device_actions (
        created_at datetime(6) not null,
        executed_at datetime(6),
        id bigint not null auto_increment,
        variable_id bigint not null,
        command_value varchar(255) not null,
        status enum ('EXECUTED','FAILED','PENDING') not null,
        primary key (id)
    ) engine=InnoDB;

    create table device_variables (
        max_value float(53),
        min_value float(53),
        device_id bigint not null,
        id bigint not null auto_increment,
        field_key varchar(255) not null,
        label varchar(255) not null,
        ui_icon varchar(255),
        unit varchar(255),
        variable_type varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table mecatronic_devices (
        is_enabled bit,
        id bigint not null auto_increment,
        last_connection datetime(6),
        product_id bigint not null,
        api_key varchar(255) not null,
        device_serial varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table notifications (
        is_read bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        last_read_by_id bigint,
        user_id bigint,
        description varchar(500) not null,
        icon varchar(255) not null,
        link varchar(255),
        title varchar(255) not null,
        category enum ('ERROR','INFO','ORDER','PROMO','SECURITY','SUCCESS','WARNING') not null,
        scope enum ('ADMIN_SHARED','CLIENT_SHARED','DELIVERER_SHARED','GLOBAL','PERSONAL') not null,
        primary key (id)
    ) engine=InnoDB;

    create table offers (
        active bit not null,
        discount_value decimal(38,2) not null,
        min_quantity integer not null,
        created_at datetime(6),
        end_date datetime(6),
        id bigint not null auto_increment,
        product_id bigint not null,
        start_date datetime(6),
        title varchar(255) not null,
        discount_type enum ('FIXED','PERCENTAGE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table otp (
        code varchar(6) not null,
        expires_at datetime(6) not null,
        id bigint not null auto_increment,
        email varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table product_categories (
        category_id bigint not null,
        product_id bigint not null,
        primary key (category_id, product_id)
    ) engine=InnoDB;

    create table product_detail_list_items (
        list_id bigint not null,
        item varchar(255)
    ) engine=InnoDB;

    create table product_detail_lists (
        id bigint not null auto_increment,
        product_id bigint,
        display_type varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table product_images (
        product_id bigint not null,
        image_url varchar(255)
    ) engine=InnoDB;

    create table product_manuals (
        product_id bigint not null,
        title varchar(255),
        url varchar(255)
    ) engine=InnoDB;

    create table products (
        average_rating float(53),
        is_active bit not null,
        is_mecatronic bit not null,
        price decimal(38,2) not null,
        purchase_price decimal(38,2),
        review_count integer,
        stock integer not null,
        version integer,
        brand_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        description TEXT,
        main_image_url varchar(255),
        name varchar(255) not null,
        slug varchar(255) not null,
        video_url varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table refresh_token (
        revoked bit not null,
        expiry_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint,
        token varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table reservation_items (
        discount_value_snapshot decimal(38,2),
        original_price decimal(38,2),
        price decimal(38,2) not null,
        price_modified bit,
        purchase_price decimal(38,2),
        quantity integer not null,
        id bigint not null auto_increment,
        offer_id_snapshot bigint,
        product_id bigint not null,
        reservation_id bigint not null,
        offer_title_snapshot varchar(255),
        product_image_snapshot varchar(255),
        product_name_snapshot varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table reservations (
        latitude float(53),
        longitude float(53),
        shipping_cost decimal(38,2),
        tax_amount decimal(10,2),
        tax_rate decimal(5,2),
        total decimal(38,2) not null,
        version integer,
        completed_at datetime(6),
        created_at datetime(6),
        deliverer_id bigint,
        id bigint not null auto_increment,
        shipped_at datetime(6),
        user_id bigint not null,
        delivery_code varchar(10),
        reference varchar(20),
        neighborhood varchar(300),
        shipping_address varchar(500),
        delivery_image_url varchar(1000),
        delivery_notes varchar(1000),
        receiver_name varchar(255),
        receiver_phone varchar(255),
        delivery_method enum ('DELIVERY','PICKUP') not null,
        status enum ('CANCELLED','COMPLETED','CONFIRMED','PENDING','PREPARING','READY_FOR_DELIVERY','READY_FOR_PICKUP','SHIPPED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table reviews (
        rating integer not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        product_id bigint not null,
        user_id bigint not null,
        comment varchar(1000),
        status enum ('APPROVED','PENDING','REJECTED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table security_logs (
        id bigint not null auto_increment,
        timestamp datetime(6) not null,
        details TEXT,
        email varchar(255),
        ip_address varchar(255),
        user_agent varchar(255),
        action enum ('ADVERTISER_CREATED','ADVERTISER_DELETED','ADVERTISER_UPDATED','AD_REQUEST_DELETED','AD_REQUEST_STATUS_CHANGED','BANNER_CREATED','BANNER_DELETED','BRAND_CREATED','BRAND_DELETED','BRAND_UPDATED','CATEGORY_CREATED','CATEGORY_DELETED','CATEGORY_UPDATED','JWT_EXPIRED','JWT_INVALID','LOGIN_FAILED','LOGIN_SUCCESS','LOGOUT','OFFER_CREATED','OFFER_DELETED','PASSWORD_RESET_REQUEST','PASSWORD_RESET_SUCCESS','PRICE_MANIPULATION_DETECTED','PRODUCT_CREATED','PRODUCT_DELETED','PRODUCT_STOCK_ADJUSTED','PRODUCT_UPDATED','RATE_LIMIT_EXCEEDED','REGISTER_SUCCESS','RESERVATION_CANCELLED','RESERVATION_STATUS_CHANGED','REVIEW_APPROVED','REVIEW_DELETED','REVIEW_REJECTED','ROLE_CHANGED','SETTING_CHANGED','UNAUTHORIZED_ACCESS','USER_ACTIVATED','USER_DEACTIVATED','USER_ROLES_UPDATED','USER_STATUS_TOGGLED','VERIFY_FAILED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table support_requests (
        created_at datetime(6),
        id bigint not null auto_increment,
        attachment_url varchar(255),
        email varchar(255) not null,
        message TEXT not null,
        name varchar(255) not null,
        order_number varchar(255),
        request_type varchar(255) not null,
        status varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_linked_devices (
        id bigint not null auto_increment,
        linked_at datetime(6),
        product_id bigint not null,
        user_id bigint not null,
        serial_number varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_roles (
        user_id bigint not null,
        roles enum ('ADMIN','CLIENT','DELIVERER','SUPER_ADMIN')
    ) engine=InnoDB;

    create table users (
        birth_date date,
        enabled boolean default true not null,
        is_verified boolean default true not null,
        verification_code varchar(6),
        code_expires_at datetime(6),
        created_at datetime(6),
        id bigint not null auto_increment,
        last_active_at datetime(6),
        document_type varchar(20),
        gender varchar(20),
        document_number varchar(30),
        email varchar(255) not null,
        name varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        profile_picture_url varchar(255),
        location varchar(255),
        default_dashboard enum ('ADMIN','CLIENT','DELIVERY'),
        status enum ('ACTIVE','INACTIVE_BY_USER','PENDING','SUSPENDED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table variable_readings (
        id bigint not null auto_increment,
        timestamp datetime(6) not null,
        variable_id bigint not null,
        value varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table advertisers 
       add constraint UK1oglpc04bi222jd4hj63jspo4 unique (name);

    alter table brands 
       add constraint UKoce3937d2f4mpfqrycbr0l93m unique (name);

    alter table categories 
       add constraint UKt8o6pivur7nn124jehx7cygw5 unique (name);

    alter table mecatronic_devices 
       add constraint UK5jojyjuy45u5rx99wvltbbnwe unique (product_id);

    alter table mecatronic_devices 
       add constraint UKapf5i2mb14v59rsldewku8u1b unique (api_key);

    alter table mecatronic_devices 
       add constraint UK6hq4st4uh13kd8w7cnr6ybwjw unique (device_serial);

    alter table products 
       add constraint UKostq1ec3toafnjok09y9l7dox unique (slug);

    alter table refresh_token 
       add constraint UKr4k4edos30bx9neoq81mdvwph unique (token);

    alter table reservations 
       add constraint UKr7sngw1pfskqivl2hyltfis5 unique (reference);

    alter table users 
       add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

    alter table ad_metrics 
       add constraint FK4cwyupwulboauj34y729i7o8q 
       foreign key (advertiser_id) 
       references advertisers (id);

    alter table addresses 
       add constraint FK1fa36y2oqhao3wgg2rw1pi459 
       foreign key (user_id) 
       references users (id);

    alter table banner_metrics 
       add constraint FKsv2k6yv6af976tjql48h03xfd 
       foreign key (banner_id) 
       references banners (id);

    alter table device_actions 
       add constraint FKm64jinor1hxflcbfgcx5n1sgr 
       foreign key (variable_id) 
       references device_variables (id);

    alter table device_variables 
       add constraint FKld0l04fdhqokx4ta4g140ekrs 
       foreign key (device_id) 
       references mecatronic_devices (id);

    alter table mecatronic_devices 
       add constraint FKhkprlrfk1ksiekjpubveabg0p 
       foreign key (product_id) 
       references products (id);

    alter table notifications 
       add constraint FKmi7qkmsk8sf0fbbvg01wqkmm7 
       foreign key (last_read_by_id) 
       references users (id);

    alter table notifications 
       add constraint FK9y21adhxn0ayjhfocscqox7bh 
       foreign key (user_id) 
       references users (id);

    alter table offers 
       add constraint FKjf1jh3h4v4m7diel8vvhmuqas 
       foreign key (product_id) 
       references products (id);

    alter table product_categories 
       add constraint FKd112rx0alycddsms029iifrih 
       foreign key (category_id) 
       references categories (id);

    alter table product_categories 
       add constraint FKlda9rad6s180ha3dl1ncsp8n7 
       foreign key (product_id) 
       references products (id);

    alter table product_detail_list_items 
       add constraint FKdbmguomv3vmwkmbq1wdcpbwu4 
       foreign key (list_id) 
       references product_detail_lists (id);

    alter table product_detail_lists 
       add constraint FK62noejj2t2f6ik5kwpv55yy9g 
       foreign key (product_id) 
       references products (id);

    alter table product_images 
       add constraint FKqnq71xsohugpqwf3c9gxmsuy 
       foreign key (product_id) 
       references products (id);

    alter table product_manuals 
       add constraint FKbijebsnb4gxlr2jtmki7vmco8 
       foreign key (product_id) 
       references products (id);

    alter table products 
       add constraint FKa3a4mpsfdf4d2y6r8ra3sc8mv 
       foreign key (brand_id) 
       references brands (id);

    alter table refresh_token 
       add constraint FKjtx87i0jvq2svedphegvdwcuy 
       foreign key (user_id) 
       references users (id);

    alter table reservation_items 
       add constraint FK632fy1kupsnllv3gjle4hfkvy 
       foreign key (product_id) 
       references products (id);

    alter table reservation_items 
       add constraint FKahatpyi4mk3o5dcqt7d51r31k 
       foreign key (reservation_id) 
       references reservations (id);

    alter table reservations 
       add constraint FKktwys87vl8qeauobfjicjlvs5 
       foreign key (deliverer_id) 
       references users (id);

    alter table reservations 
       add constraint FKb5g9io5h54iwl2inkno50ppln 
       foreign key (user_id) 
       references users (id);

    alter table reviews 
       add constraint FKpl51cejpw4gy5swfar8br9ngi 
       foreign key (product_id) 
       references products (id);

    alter table reviews 
       add constraint FKcgy7qjc1r99dp117y9en6lxye 
       foreign key (user_id) 
       references users (id);

    alter table user_linked_devices 
       add constraint FKlwsgfie4kym5v3yv75438fjec 
       foreign key (product_id) 
       references products (id);

    alter table user_linked_devices 
       add constraint FKqfd39e49iqy36fi7kqbamahix 
       foreign key (user_id) 
       references users (id);

    alter table user_roles 
       add constraint FKhfh9dx7w3ubf1co1vdev94g3f 
       foreign key (user_id) 
       references users (id);

    alter table variable_readings 
       add constraint FKgcorbx46vw8ah6s6pbpda82w0 
       foreign key (variable_id) 
       references device_variables (id);

    create table ad_metrics (
        advertiser_id bigint not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        ip_address varchar(255),
        type enum ('CLICK','IMPRESSION') not null,
        primary key (id)
    ) engine=InnoDB;

    create table ad_requests (
        duration_months integer,
        created_at datetime(6),
        end_date datetime(6),
        id bigint not null auto_increment,
        start_date datetime(6),
        button_text varchar(50),
        ad_description TEXT,
        ad_image_url varchar(255),
        ad_title varchar(255),
        company_description TEXT,
        company_name varchar(255) not null,
        contact_name varchar(255) not null,
        email varchar(255) not null,
        logo_url varchar(255),
        message TEXT,
        phone varchar(255),
        redirect_url varchar(255),
        website_url varchar(255),
        placement enum ('CHECKOUT','PRODUCT_DETAIL','PRODUCT_LIST'),
        status enum ('APPROVED','EXPIRED','PENDING','REJECTED'),
        primary key (id)
    ) engine=InnoDB;

    create table addresses (
        is_default boolean default false not null,
        latitude float(53),
        longitude float(53),
        id bigint not null auto_increment,
        user_id bigint not null,
        receiver_phone varchar(50),
        title varchar(50) not null,
        apartment_office varchar(100),
        city varchar(100) not null,
        country varchar(100),
        department varchar(100),
        neighborhood varchar(100),
        receiver_name varchar(100),
        street varchar(200) not null,
        image_url varchar(500),
        reference varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table advertisers (
        active bit not null,
        ad_order integer,
        duration_months integer,
        clicks_count bigint,
        end_date datetime(6),
        id bigint not null auto_increment,
        start_date datetime(6),
        views_count bigint,
        button_text varchar(50),
        ad_description TEXT,
        ad_image_url varchar(255),
        ad_title varchar(255),
        company_description TEXT,
        contact_email varchar(255),
        contact_name varchar(255),
        logo_url varchar(255),
        name varchar(255) not null,
        phone varchar(255),
        redirect_url varchar(255),
        website_url varchar(255),
        placement enum ('CHECKOUT','PRODUCT_DETAIL','PRODUCT_LIST'),
        primary key (id)
    ) engine=InnoDB;

    create table app_settings (
        base_distance_km float(53),
        cost_per_km decimal(10,2),
        default_shipping_cost decimal(38,2),
        distance_shipping_enabled bit,
        free_shipping_threshold decimal(38,2),
        max_image_size_mb integer,
        max_pdf_size_mb integer,
        store_latitude float(53),
        store_longitude float(53),
        tax_enabled bit,
        tax_rate decimal(5,2),
        id bigint not null auto_increment,
        address varchar(500),
        footer_text varchar(500),
        meta_description varchar(500),
        meta_keywords varchar(500),
        bg_dark_url varchar(255),
        bg_light_url varchar(255),
        contact_email varchar(255),
        contact_phone varchar(255),
        copyright_text varchar(255),
        facebook_url varchar(255),
        favicon_url varchar(255),
        instagram_url varchar(255),
        logo_url varchar(255),
        meta_title varchar(255),
        store_name varchar(255) not null,
        tagline varchar(255),
        whatsapp_number varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table banner_metrics (
        banner_id bigint not null,
        created_at datetime(6),
        id bigint not null auto_increment,
        ip_address varchar(255),
        type enum ('CLICK','IMPRESSION') not null,
        primary key (id)
    ) engine=InnoDB;

    create table banners (
        active bit not null,
        banner_order integer,
        clicks_count bigint,
        end_date datetime(6),
        id bigint not null auto_increment,
        start_date datetime(6),
        views_count bigint,
        button_text varchar(50),
        description TEXT,
        image_url varchar(255) not null,
        link_url varchar(255),
        title varchar(255),
        placement enum ('CHECKOUT','HOME_CAROUSEL','PRODUCT_DETAIL','SIDEBAR') not null,
        primary key (id)
    ) engine=InnoDB;

    create table brands (
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table categories (
        id bigint not null auto_increment,
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table device_actions (
        created_at datetime(6) not null,
        executed_at datetime(6),
        id bigint not null auto_increment,
        variable_id bigint not null,
        command_value varchar(255) not null,
        status enum ('EXECUTED','FAILED','PENDING') not null,
        primary key (id)
    ) engine=InnoDB;

    create table device_variables (
        max_value float(53),
        min_value float(53),
        device_id bigint not null,
        id bigint not null auto_increment,
        field_key varchar(255) not null,
        label varchar(255) not null,
        ui_icon varchar(255),
        unit varchar(255),
        variable_type varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table mecatronic_devices (
        is_enabled bit,
        id bigint not null auto_increment,
        last_connection datetime(6),
        product_id bigint not null,
        api_key varchar(255) not null,
        device_serial varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table notifications (
        is_read bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        last_read_by_id bigint,
        user_id bigint,
        description varchar(500) not null,
        icon varchar(255) not null,
        link varchar(255),
        title varchar(255) not null,
        category enum ('ERROR','INFO','ORDER','PROMO','SECURITY','SUCCESS','WARNING') not null,
        scope enum ('ADMIN_SHARED','CLIENT_SHARED','DELIVERER_SHARED','GLOBAL','PERSONAL') not null,
        primary key (id)
    ) engine=InnoDB;

    create table offers (
        active bit not null,
        discount_value decimal(38,2) not null,
        min_quantity integer not null,
        created_at datetime(6),
        end_date datetime(6),
        id bigint not null auto_increment,
        product_id bigint not null,
        start_date datetime(6),
        title varchar(255) not null,
        discount_type enum ('FIXED','PERCENTAGE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table otp (
        code varchar(6) not null,
        expires_at datetime(6) not null,
        id bigint not null auto_increment,
        email varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table product_categories (
        category_id bigint not null,
        product_id bigint not null,
        primary key (category_id, product_id)
    ) engine=InnoDB;

    create table product_detail_list_items (
        list_id bigint not null,
        item varchar(255)
    ) engine=InnoDB;

    create table product_detail_lists (
        id bigint not null auto_increment,
        product_id bigint,
        display_type varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table product_images (
        product_id bigint not null,
        image_url varchar(255)
    ) engine=InnoDB;

    create table product_manuals (
        product_id bigint not null,
        title varchar(255),
        url varchar(255)
    ) engine=InnoDB;

    create table products (
        average_rating float(53),
        is_active bit not null,
        is_mecatronic bit not null,
        price decimal(38,2) not null,
        purchase_price decimal(38,2),
        review_count integer,
        stock integer not null,
        version integer,
        brand_id bigint,
        created_at datetime(6),
        id bigint not null auto_increment,
        description TEXT,
        main_image_url varchar(255),
        name varchar(255) not null,
        slug varchar(255) not null,
        video_url varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table refresh_token (
        revoked bit not null,
        expiry_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint,
        token varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table reservation_items (
        discount_value_snapshot decimal(38,2),
        original_price decimal(38,2),
        price decimal(38,2) not null,
        price_modified bit,
        purchase_price decimal(38,2),
        quantity integer not null,
        id bigint not null auto_increment,
        offer_id_snapshot bigint,
        product_id bigint not null,
        reservation_id bigint not null,
        offer_title_snapshot varchar(255),
        product_image_snapshot varchar(255),
        product_name_snapshot varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table reservations (
        latitude float(53),
        longitude float(53),
        shipping_cost decimal(38,2),
        tax_amount decimal(10,2),
        tax_rate decimal(5,2),
        total decimal(38,2) not null,
        version integer,
        completed_at datetime(6),
        created_at datetime(6),
        deliverer_id bigint,
        id bigint not null auto_increment,
        shipped_at datetime(6),
        user_id bigint not null,
        delivery_code varchar(10),
        reference varchar(20),
        neighborhood varchar(300),
        shipping_address varchar(500),
        delivery_image_url varchar(1000),
        delivery_notes varchar(1000),
        receiver_name varchar(255),
        receiver_phone varchar(255),
        delivery_method enum ('DELIVERY','PICKUP') not null,
        status enum ('CANCELLED','COMPLETED','CONFIRMED','PENDING','PREPARING','READY_FOR_DELIVERY','READY_FOR_PICKUP','SHIPPED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table reviews (
        rating integer not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        product_id bigint not null,
        user_id bigint not null,
        comment varchar(1000),
        status enum ('APPROVED','PENDING','REJECTED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table security_logs (
        id bigint not null auto_increment,
        timestamp datetime(6) not null,
        details TEXT,
        email varchar(255),
        ip_address varchar(255),
        user_agent varchar(255),
        action enum ('ADVERTISER_CREATED','ADVERTISER_DELETED','ADVERTISER_UPDATED','AD_REQUEST_DELETED','AD_REQUEST_STATUS_CHANGED','BANNER_CREATED','BANNER_DELETED','BRAND_CREATED','BRAND_DELETED','BRAND_UPDATED','CATEGORY_CREATED','CATEGORY_DELETED','CATEGORY_UPDATED','JWT_EXPIRED','JWT_INVALID','LOGIN_FAILED','LOGIN_SUCCESS','LOGOUT','OFFER_CREATED','OFFER_DELETED','PASSWORD_RESET_REQUEST','PASSWORD_RESET_SUCCESS','PRICE_MANIPULATION_DETECTED','PRODUCT_CREATED','PRODUCT_DELETED','PRODUCT_STOCK_ADJUSTED','PRODUCT_UPDATED','RATE_LIMIT_EXCEEDED','REGISTER_SUCCESS','RESERVATION_CANCELLED','RESERVATION_STATUS_CHANGED','REVIEW_APPROVED','REVIEW_DELETED','REVIEW_REJECTED','ROLE_CHANGED','SETTING_CHANGED','UNAUTHORIZED_ACCESS','USER_ACTIVATED','USER_DEACTIVATED','USER_ROLES_UPDATED','USER_STATUS_TOGGLED','VERIFY_FAILED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table support_requests (
        created_at datetime(6),
        id bigint not null auto_increment,
        attachment_url varchar(255),
        email varchar(255) not null,
        message TEXT not null,
        name varchar(255) not null,
        order_number varchar(255),
        request_type varchar(255) not null,
        status varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_linked_devices (
        id bigint not null auto_increment,
        linked_at datetime(6),
        product_id bigint not null,
        user_id bigint not null,
        serial_number varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user_roles (
        user_id bigint not null,
        roles enum ('ADMIN','CLIENT','DELIVERER','SUPER_ADMIN')
    ) engine=InnoDB;

    create table users (
        birth_date date,
        enabled boolean default true not null,
        is_verified boolean default true not null,
        verification_code varchar(6),
        code_expires_at datetime(6),
        created_at datetime(6),
        id bigint not null auto_increment,
        last_active_at datetime(6),
        document_type varchar(20),
        gender varchar(20),
        document_number varchar(30),
        email varchar(255) not null,
        name varchar(255) not null,
        password varchar(255) not null,
        phone varchar(255),
        profile_picture_url varchar(255),
        default_dashboard enum ('ADMIN','CLIENT','DELIVERY'),
        status enum ('ACTIVE','INACTIVE_BY_USER','PENDING','SUSPENDED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table variable_readings (
        id bigint not null auto_increment,
        timestamp datetime(6) not null,
        variable_id bigint not null,
        value varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table advertisers 
       add constraint UK1oglpc04bi222jd4hj63jspo4 unique (name);

    alter table brands 
       add constraint UKoce3937d2f4mpfqrycbr0l93m unique (name);

    alter table categories 
       add constraint UKt8o6pivur7nn124jehx7cygw5 unique (name);

    alter table mecatronic_devices 
       add constraint UK5jojyjuy45u5rx99wvltbbnwe unique (product_id);

    alter table mecatronic_devices 
       add constraint UKapf5i2mb14v59rsldewku8u1b unique (api_key);

    alter table mecatronic_devices 
       add constraint UK6hq4st4uh13kd8w7cnr6ybwjw unique (device_serial);

    alter table products 
       add constraint UKostq1ec3toafnjok09y9l7dox unique (slug);

    alter table refresh_token 
       add constraint UKr4k4edos30bx9neoq81mdvwph unique (token);

    alter table reservations 
       add constraint UKr7sngw1pfskqivl2hyltfis5 unique (reference);

    alter table users 
       add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

    alter table ad_metrics 
       add constraint FK4cwyupwulboauj34y729i7o8q 
       foreign key (advertiser_id) 
       references advertisers (id);

    alter table addresses 
       add constraint FK1fa36y2oqhao3wgg2rw1pi459 
       foreign key (user_id) 
       references users (id);

    alter table banner_metrics 
       add constraint FKsv2k6yv6af976tjql48h03xfd 
       foreign key (banner_id) 
       references banners (id);

    alter table device_actions 
       add constraint FKm64jinor1hxflcbfgcx5n1sgr 
       foreign key (variable_id) 
       references device_variables (id);

    alter table device_variables 
       add constraint FKld0l04fdhqokx4ta4g140ekrs 
       foreign key (device_id) 
       references mecatronic_devices (id);

    alter table mecatronic_devices 
       add constraint FKhkprlrfk1ksiekjpubveabg0p 
       foreign key (product_id) 
       references products (id);

    alter table notifications 
       add constraint FKmi7qkmsk8sf0fbbvg01wqkmm7 
       foreign key (last_read_by_id) 
       references users (id);

    alter table notifications 
       add constraint FK9y21adhxn0ayjhfocscqox7bh 
       foreign key (user_id) 
       references users (id);

    alter table offers 
       add constraint FKjf1jh3h4v4m7diel8vvhmuqas 
       foreign key (product_id) 
       references products (id);

    alter table product_categories 
       add constraint FKd112rx0alycddsms029iifrih 
       foreign key (category_id) 
       references categories (id);

    alter table product_categories 
       add constraint FKlda9rad6s180ha3dl1ncsp8n7 
       foreign key (product_id) 
       references products (id);

    alter table product_detail_list_items 
       add constraint FKdbmguomv3vmwkmbq1wdcpbwu4 
       foreign key (list_id) 
       references product_detail_lists (id);

    alter table product_detail_lists 
       add constraint FK62noejj2t2f6ik5kwpv55yy9g 
       foreign key (product_id) 
       references products (id);

    alter table product_images 
       add constraint FKqnq71xsohugpqwf3c9gxmsuy 
       foreign key (product_id) 
       references products (id);

    alter table product_manuals 
       add constraint FKbijebsnb4gxlr2jtmki7vmco8 
       foreign key (product_id) 
       references products (id);

    alter table products 
       add constraint FKa3a4mpsfdf4d2y6r8ra3sc8mv 
       foreign key (brand_id) 
       references brands (id);

    alter table refresh_token 
       add constraint FKjtx87i0jvq2svedphegvdwcuy 
       foreign key (user_id) 
       references users (id);

    alter table reservation_items 
       add constraint FK632fy1kupsnllv3gjle4hfkvy 
       foreign key (product_id) 
       references products (id);

    alter table reservation_items 
       add constraint FKahatpyi4mk3o5dcqt7d51r31k 
       foreign key (reservation_id) 
       references reservations (id);

    alter table reservations 
       add constraint FKktwys87vl8qeauobfjicjlvs5 
       foreign key (deliverer_id) 
       references users (id);

    alter table reservations 
       add constraint FKb5g9io5h54iwl2inkno50ppln 
       foreign key (user_id) 
       references users (id);

    alter table reviews 
       add constraint FKpl51cejpw4gy5swfar8br9ngi 
       foreign key (product_id) 
       references products (id);

    alter table reviews 
       add constraint FKcgy7qjc1r99dp117y9en6lxye 
       foreign key (user_id) 
       references users (id);

    alter table user_linked_devices 
       add constraint FKlwsgfie4kym5v3yv75438fjec 
       foreign key (product_id) 
       references products (id);

    alter table user_linked_devices 
       add constraint FKqfd39e49iqy36fi7kqbamahix 
       foreign key (user_id) 
       references users (id);

    alter table user_roles 
       add constraint FKhfh9dx7w3ubf1co1vdev94g3f 
       foreign key (user_id) 
       references users (id);

    alter table variable_readings 
       add constraint FKgcorbx46vw8ah6s6pbpda82w0 
       foreign key (variable_id) 
       references device_variables (id);
