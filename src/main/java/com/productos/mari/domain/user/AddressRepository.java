package com.productos.mari.domain.user;

import com.productos.mari.domain.user.Address;
import com.productos.mari.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUser(User user);
    
    // Si queremos facilitar el update de isDefault, podemos hacer consultas custom,
    // o simplemente manejarlas en el Service.
}
