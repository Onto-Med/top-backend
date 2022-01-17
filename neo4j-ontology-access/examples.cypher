CREATE
    (super:Class { uuid: '3fa85f64-5717-4562-b3fc-2c963f66afa6', title_de: 'Gewicht', title_en: 'Weight', unit: 'kg', datatype: 'decimal' })
        -[:SUB_CLASS]->
        (sub:Class { uuid: '4fa85f64-5717-4562-b3fc-2c963f66afa7', title_de: 'Gewicht > 100 kg', title_en: 'Weight > 100 kg' }),
    (sub) -[:SUPER_CLASS]-> (super);